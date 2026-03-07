package ru.coderoom.course.code

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException
import ru.coderoom.course.domain.CodeLanguage
import java.math.BigDecimal
import java.math.RoundingMode

enum class CoderoomCheckType {
    UNIT_TESTS,
    SCRIPT,
}

enum class CoderoomScoringStrategy {
    ALL_OR_NOTHING,
    PASS_RATE,
}

data class CoderoomConfig(
    val version: Int = 1,
    val language: CodeLanguage,
    val check: CoderoomCheckConfig,
    val attempts: CoderoomAttemptsConfig,
    val privateTests: CoderoomPrivateTestsConfig,
    val scoring: CoderoomScoringConfig,
)

data class CoderoomCheckConfig(
    val type: CoderoomCheckType,
    val command: String,
    val workdir: String = ".",
)

data class CoderoomAttemptsConfig(
    val max: Int,
)

data class CoderoomPrivateTestsConfig(
    val enabled: Boolean = false,
    val targetPath: String = ".",
)

data class CoderoomScoringConfig(
    val strategy: CoderoomScoringStrategy = CoderoomScoringStrategy.ALL_OR_NOTHING,
    val maxScore: BigDecimal = BigDecimal("100.00"),
)

@Component
class CoderoomConfigMapper {
    private val mapper: ObjectMapper =
        ObjectMapper(YAMLFactory())
            .registerModule(KotlinModule.Builder().build())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)

    fun createDefault(
        language: CodeLanguage,
        maxAttempts: Int,
        privateTestsEnabled: Boolean,
    ): String =
        write(
            CoderoomConfig(
                language = language,
                check = CoderoomCheckConfig(
                    type = CoderoomCheckType.UNIT_TESTS,
                    command = defaultCommand(language),
                    workdir = ".",
                ),
                attempts = CoderoomAttemptsConfig(max = maxAttempts),
                privateTests = CoderoomPrivateTestsConfig(
                    enabled = privateTestsEnabled,
                    targetPath = defaultPrivateTestsTargetPath(language),
                ),
                scoring = CoderoomScoringConfig(
                    strategy = CoderoomScoringStrategy.PASS_RATE,
                    maxScore = BigDecimal("100.00"),
                ),
            ),
        )

    fun parseAndValidate(rawYaml: String): CoderoomConfig {
        val config = try {
            mapper.readValue<CoderoomConfig>(rawYaml)
        } catch (ex: Exception) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid .coderoom.yml: ${ex.message}")
        }
        validate(config)
        return config
    }

    fun write(config: CoderoomConfig): String =
        mapper.writeValueAsString(config).trim() + System.lineSeparator()

    private fun validate(config: CoderoomConfig) {
        if (config.version != 1) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported .coderoom.yml version")
        }
        if (config.check.command.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "check.command must not be blank")
        }
        if (config.check.workdir.isBlank() || config.check.workdir.startsWith("/") || config.check.workdir.contains("..")) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "check.workdir must be a safe relative path")
        }
        if (config.attempts.max <= 0) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "attempts.max must be positive")
        }
        if (config.privateTests.enabled && config.privateTests.targetPath.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "privateTests.targetPath must not be blank")
        }
        if (config.scoring.maxScore <= BigDecimal.ZERO) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "scoring.maxScore must be positive")
        }
    }

    private fun defaultCommand(language: CodeLanguage): String =
        when (language) {
            CodeLanguage.GO -> "go test -json ./..."
            CodeLanguage.PYTHON -> "python -m pytest --junitxml=.coderoom/junit.xml"
            CodeLanguage.JAVA -> "./gradlew test"
        }

    private fun defaultPrivateTestsTargetPath(language: CodeLanguage): String =
        when (language) {
            CodeLanguage.GO -> "."
            CodeLanguage.PYTHON -> "tests"
            CodeLanguage.JAVA -> "src/test/java"
        }
}

fun BigDecimal.normalizedScoreScale(): BigDecimal = setScale(2, RoundingMode.HALF_UP)
