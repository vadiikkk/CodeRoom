package ru.coderoom.runner.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "coderoom.kafka.topics")
data class KafkaTopicsProperties(
    val runnerJobs: String = "coderoom.runner.jobs",
    val runnerResults: String = "coderoom.runner.results",
)

@ConfigurationProperties(prefix = "coderoom.content")
data class ContentProperties(
    val baseUrl: String = "http://content-service:8080",
)

@ConfigurationProperties(prefix = "coderoom.runner")
data class RunnerProperties(
    val workspaceRoot: String = "/tmp/coderoom-runs",
    val commandTimeoutSeconds: Long = 180,
)
