package ru.coderoom.autograding.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.coderoom.autograding.domain.TestRunEntity
import ru.coderoom.autograding.domain.TestRunStatus
import ru.coderoom.autograding.messaging.AutogradeFinishedMessage
import ru.coderoom.autograding.messaging.AutogradeRequestedMessage
import ru.coderoom.autograding.messaging.KafkaPublishers
import ru.coderoom.autograding.messaging.RunTestRunMessage
import ru.coderoom.autograding.messaging.RunnerFinishedMessage
import ru.coderoom.autograding.repo.TestRunRepository
import java.time.Instant
import java.util.UUID

@Service
class AutogradingService(
    private val testRuns: TestRunRepository,
    private val publishers: KafkaPublishers,
) {
    @Transactional
    fun enqueue(message: AutogradeRequestedMessage) {
        val existing = testRuns.findByAttemptId(message.attemptId)
        if (existing != null) {
            existing.status = TestRunStatus.QUEUED
            existing.score = null
            existing.comment = null
            existing.resultSummary = null
            existing.logObjectKey = null
            existing.exitCode = null
            existing.testsPassed = null
            existing.testsTotal = null
            existing.scoringMode = null
            existing.startedAt = null
            existing.finishedAt = null
            existing.queuedAt = message.queuedAt
            existing.updatedAt = Instant.now()
            testRuns.save(existing)
            publishers.publishRunnerJob(
                RunTestRunMessage(
                    testRunId = existing.testRunId,
                    attemptId = existing.attemptId,
                    language = existing.language,
                    repositoryFullName = message.repositoryFullName,
                    repositoryCloneUrl = message.repositoryCloneUrl,
                    defaultBranch = message.defaultBranch,
                    pullRequestUrl = message.pullRequestUrl,
                    pullRequestNumber = message.pullRequestNumber,
                    pullRequestHeadSha = message.pullRequestHeadSha,
                    configSnapshot = message.configSnapshot,
                    privateTests = message.privateTests,
                ),
            )
            return
        }
        val now = Instant.now()
        val testRun = testRuns.save(
            TestRunEntity(
                testRunId = UUID.randomUUID(),
                attemptId = message.attemptId,
                courseId = message.courseId,
                assignmentId = message.assignmentId,
                studentUserId = message.studentUserId,
                language = message.language,
                repositoryFullName = message.repositoryFullName,
                pullRequestUrl = message.pullRequestUrl,
                pullRequestNumber = message.pullRequestNumber,
                pullRequestHeadSha = message.pullRequestHeadSha,
                configSnapshot = message.configSnapshot,
                status = TestRunStatus.QUEUED,
                privateTestsObjectKey = message.privateTests?.objectKey,
                queuedAt = message.queuedAt,
                createdAt = now,
                updatedAt = now,
            ),
        )
        publishers.publishRunnerJob(
            RunTestRunMessage(
                testRunId = testRun.testRunId,
                attemptId = testRun.attemptId,
                language = testRun.language,
                repositoryFullName = message.repositoryFullName,
                repositoryCloneUrl = message.repositoryCloneUrl,
                defaultBranch = message.defaultBranch,
                pullRequestUrl = message.pullRequestUrl,
                pullRequestNumber = message.pullRequestNumber,
                pullRequestHeadSha = message.pullRequestHeadSha,
                configSnapshot = message.configSnapshot,
                privateTests = message.privateTests,
            ),
        )
    }

    @Transactional
    fun complete(message: RunnerFinishedMessage) {
        val testRun = testRuns.findById(message.testRunId).orElse(null) ?: return
        testRun.status = message.status
        testRun.score = message.score
        testRun.comment = message.comment
        testRun.resultSummary = message.resultSummary
        testRun.logObjectKey = message.logObjectKey
        testRun.exitCode = message.exitCode
        testRun.testsPassed = message.testsPassed
        testRun.testsTotal = message.testsTotal
        testRun.scoringMode = message.scoringMode
        testRun.startedAt = message.startedAt
        testRun.finishedAt = message.finishedAt
        testRun.updatedAt = Instant.now()
        testRuns.save(testRun)
        publishers.publishAutogradeResult(
            AutogradeFinishedMessage(
                attemptId = testRun.attemptId,
                status = message.status,
                score = message.score,
                comment = message.comment,
                resultSummary = message.resultSummary,
                logObjectKey = message.logObjectKey,
                exitCode = message.exitCode,
                testsPassed = message.testsPassed,
                testsTotal = message.testsTotal,
                scoringMode = message.scoringMode,
                startedAt = message.startedAt,
                finishedAt = message.finishedAt,
            ),
        )
    }
}
