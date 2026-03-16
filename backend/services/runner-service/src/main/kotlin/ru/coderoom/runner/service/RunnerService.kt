package ru.coderoom.runner.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import ru.coderoom.runner.code.CoderoomConfig
import ru.coderoom.runner.code.CoderoomConfigMapper
import ru.coderoom.runner.code.CoderoomScoringStrategy
import ru.coderoom.runner.config.RunnerProperties
import ru.coderoom.runner.content.ContentClient
import ru.coderoom.runner.executor.LanguageExecutor
import ru.coderoom.runner.messaging.CodeLanguage
import ru.coderoom.runner.messaging.RunTestRunMessage
import ru.coderoom.runner.messaging.RunnerFinishedMessage
import ru.coderoom.runner.messaging.TestRunStatus
import java.io.ByteArrayInputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.concurrent.thread

@Service
class RunnerService(
    private val content: ContentClient,
    private val configMapper: CoderoomConfigMapper,
    private val objectMapper: ObjectMapper,
    private val executors: List<LanguageExecutor>,
    private val runnerProperties: RunnerProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun run(message: RunTestRunMessage): RunnerFinishedMessage {
        val startedAt = Instant.now()
        val workspaceRoot = Paths.get(runnerProperties.workspaceRoot).resolve(message.testRunId.toString())
        val repoDir = workspaceRoot.resolve("repo")
        var mergedOutput = ""
        return try {
            Files.createDirectories(workspaceRoot)
            val config = configMapper.parse(message.configSnapshot)
            requireExecutor(message.language).prepareWorkspace(workspaceRoot)
            runCommand(listOf("git", "clone", "--depth", "1", message.repositoryCloneUrl, repoDir.toString()), workspaceRoot, 60)
            runCommand(
                listOf("git", "-C", repoDir.toString(), "fetch", "origin", "pull/${message.pullRequestNumber}/head:coderoom-pr-${message.pullRequestNumber}"),
                workspaceRoot,
                60,
            )
            runCommand(listOf("git", "-C", repoDir.toString(), "checkout", "coderoom-pr-${message.pullRequestNumber}"), workspaceRoot, 60)

            if (message.privateTests != null && config.privateTests.enabled) {
                val zipBytes = content.downloadBytes(message.privateTests.objectKey, message.privateTests.fileName)
                unzip(zipBytes, repoDir.resolve(config.privateTests.targetPath))
            }

            val workdir = repoDir.resolve(config.check.workdir).normalize()
            Files.createDirectories(workdir)
            val commandResult = runShellCommand(config.check.command, workdir, runnerProperties.commandTimeoutSeconds)
            mergedOutput = commandResult.output
            val scoring = scoreRun(config, message.language, commandResult.output, commandResult.exitCode, repoDir)
            val logObjectKey = content.uploadBytes(
                objectKey = "autograding/test-runs/${message.testRunId}/runner.log",
                contentType = "text/plain",
                bytes = commandResult.output.toByteArray(StandardCharsets.UTF_8),
            )
            RunnerFinishedMessage(
                testRunId = message.testRunId,
                attemptId = message.attemptId,
                status = TestRunStatus.COMPLETED,
                score = scoring.score,
                comment = scoring.comment,
                resultSummary = scoring.summary,
                logObjectKey = logObjectKey,
                exitCode = commandResult.exitCode,
                testsPassed = scoring.testsPassed,
                testsTotal = scoring.testsTotal,
                scoringMode = scoring.scoringMode,
                startedAt = startedAt,
                finishedAt = Instant.now(),
            )
        } catch (ex: Exception) {
            log.error("Runner job failed for testRunId={} attemptId={}", message.testRunId, message.attemptId, ex)
            val failureMessage = ex.message ?: ex.javaClass.simpleName
            val errorLog = buildString {
                appendLine("Runner execution failed")
                appendLine(failureMessage)
                if (mergedOutput.isNotBlank()) {
                    appendLine()
                    appendLine(mergedOutput)
                }
            }
            val logObjectKey = runCatching {
                content.uploadBytes(
                    objectKey = "autograding/test-runs/${message.testRunId}/runner-error.log",
                    contentType = "text/plain",
                    bytes = errorLog.toByteArray(StandardCharsets.UTF_8),
                )
            }.getOrNull()
            RunnerFinishedMessage(
                testRunId = message.testRunId,
                attemptId = message.attemptId,
                status = TestRunStatus.ERROR,
                score = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                comment = failureMessage,
                resultSummary = "Runner execution failed: $failureMessage",
                logObjectKey = logObjectKey,
                exitCode = null,
                testsPassed = null,
                testsTotal = null,
                scoringMode = null,
                startedAt = startedAt,
                finishedAt = Instant.now(),
            )
        } finally {
            runCatching { workspaceRoot.toFile().deleteRecursively() }
        }
    }

    private fun requireExecutor(language: CodeLanguage): LanguageExecutor =
        executors.firstOrNull { it.supports(language) } ?: error("No executor configured for $language")

    private fun runShellCommand(command: String, workingDir: Path, timeoutSeconds: Long): ProcessResult {
        val process = ProcessBuilder("sh", "-lc", command)
            .directory(workingDir.toFile())
            .redirectErrorStream(true)
            .start()
        val outputHolder = StringBuilder()
        val readerThread = thread(start = true) {
            process.inputStream.bufferedReader(StandardCharsets.UTF_8).useLines { lines ->
                lines.forEach {
                    outputHolder.appendLine(it)
                }
            }
        }
        if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            readerThread.join(1_000)
            error("Command timed out after ${timeoutSeconds}s")
        }
        readerThread.join(1_000)
        return ProcessResult(exitCode = process.exitValue(), output = outputHolder.toString())
    }

    private fun runCommand(command: List<String>, workingDir: Path, timeoutSeconds: Long) {
        val process = ProcessBuilder(command)
            .directory(workingDir.toFile())
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader(StandardCharsets.UTF_8).readText()
        if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            error("Command timed out: ${command.joinToString(" ")}")
        }
        if (process.exitValue() != 0) {
            error("Command failed: ${command.joinToString(" ")}\n$output")
        }
    }

    private fun unzip(bytes: ByteArray, targetDir: Path) {
        Files.createDirectories(targetDir)
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val resolved = targetDir.resolve(entry.name).normalize()
                if (!resolved.startsWith(targetDir)) {
                    error("Unsafe zip entry path")
                }
                if (entry.isDirectory) {
                    Files.createDirectories(resolved)
                } else {
                    Files.createDirectories(resolved.parent)
                    Files.write(resolved, zip.readAllBytes())
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
    }

    private fun scoreRun(
        config: CoderoomConfig,
        language: CodeLanguage,
        output: String,
        exitCode: Int,
        repoDir: Path,
    ): ScoreResult {
        val maxScore = config.scoring.maxScore.setScale(2, RoundingMode.HALF_UP)
        val scoringMode = config.scoring.strategy.name
        if (config.scoring.strategy == CoderoomScoringStrategy.ALL_OR_NOTHING) {
            val score = if (exitCode == 0) maxScore else BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
            val summary = if (exitCode == 0) "All checks passed" else "Checks failed (exit code $exitCode)"
            return ScoreResult(
                score = score,
                summary = summary,
                comment = summary,
                testsPassed = null,
                testsTotal = null,
                scoringMode = scoringMode,
            )
        }
        val testCount = when (language) {
            CodeLanguage.GO -> parseGoJson(output)
            CodeLanguage.PYTHON, CodeLanguage.JAVA -> parseJunit(repoDir)
        }
        if (testCount == null || testCount.total <= 0) {
            val score = if (exitCode == 0) maxScore else BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
            val summary = if (exitCode == 0) "Checks passed" else "Checks failed (exit code $exitCode)"
            return ScoreResult(
                score = score,
                summary = summary,
                comment = summary,
                testsPassed = null,
                testsTotal = null,
                scoringMode = scoringMode,
            )
        }
        val score = maxScore
            .multiply(BigDecimal(testCount.passed))
            .divide(BigDecimal(testCount.total), 2, RoundingMode.HALF_UP)
        val summary = "Passed ${testCount.passed}/${testCount.total} tests"
        return ScoreResult(
            score = score,
            summary = summary,
            comment = summary,
            testsPassed = testCount.passed,
            testsTotal = testCount.total,
            scoringMode = scoringMode,
        )
    }

    private fun parseGoJson(output: String): TestCount? {
        val states = linkedMapOf<String, String>()
        output.lineSequence()
            .filter { it.isNotBlank() }
            .forEach { line ->
                runCatching { objectMapper.readTree(line) }.getOrNull()?.let { json ->
                    val testName = json.get("Test")?.asText()
                    val action = json.get("Action")?.asText()
                    if (!testName.isNullOrBlank() && (action == "pass" || action == "fail" || action == "skip")) {
                        states[testName] = action
                    }
                }
            }
        if (states.isEmpty()) return null
        val total = states.size
        val passed = states.values.count { it == "pass" }
        return TestCount(passed = passed, total = total)
    }

    private fun parseJunit(repoDir: Path): TestCount? {
        val xmlFiles = Files.walk(repoDir).use { stream ->
            stream
                .filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".xml") }
                .toList()
        }
        if (xmlFiles.isEmpty()) return null
        var total = 0
        var passed = 0
        val factory = DocumentBuilderFactory.newInstance()
        xmlFiles.forEach { file ->
            runCatching {
                val doc = factory.newDocumentBuilder().parse(file.toFile())
                val root = doc.documentElement
                val tests = root.getAttribute("tests").toIntOrNull()
                val failures = root.getAttribute("failures").toIntOrNull() ?: 0
                val errors = root.getAttribute("errors").toIntOrNull() ?: 0
                val skipped = root.getAttribute("skipped").toIntOrNull() ?: 0
                if (tests != null) {
                    total += tests
                    passed += (tests - failures - errors - skipped).coerceAtLeast(0)
                }
            }
        }
        return if (total > 0) TestCount(passed, total) else null
    }
}

data class ProcessResult(
    val exitCode: Int,
    val output: String,
)

data class TestCount(
    val passed: Int,
    val total: Int,
)

data class ScoreResult(
    val score: BigDecimal,
    val summary: String,
    val comment: String,
    val testsPassed: Int?,
    val testsTotal: Int?,
    val scoringMode: String,
)
