package ru.coderoom.autograding.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "coderoom.kafka.topics")
data class KafkaTopicsProperties(
    val autogradeJobs: String = "coderoom.autograde.jobs",
    val runnerJobs: String = "coderoom.runner.jobs",
    val runnerResults: String = "coderoom.runner.results",
    val autogradeResults: String = "coderoom.autograde.results",
)
