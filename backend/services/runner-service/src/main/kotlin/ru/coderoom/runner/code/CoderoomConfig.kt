package ru.coderoom.runner.code

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException
import ru.coderoom.runner.messaging.CodeLanguage
import java.math.BigDecimal

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
    val targetPath: String = ".coderoom/private-tests",
)

data class CoderoomScoringConfig(
    val strategy: CoderoomScoringStrategy,
    val maxScore: BigDecimal,
)

@Component
class CoderoomConfigMapper {
    private val mapper: ObjectMapper =
        ObjectMapper(YAMLFactory())
            .registerModule(KotlinModule.Builder().build())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)

    fun parse(rawYaml: String): CoderoomConfig {
        val config = try {
            mapper.readValue<CoderoomConfig>(rawYaml)
        } catch (ex: Exception) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid .coderoom.yml: ${ex.message}")
        }
        if (config.version != 1) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported .coderoom.yml version")
        }
        return config
    }
}
