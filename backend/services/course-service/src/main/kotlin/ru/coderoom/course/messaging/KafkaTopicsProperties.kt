package ru.coderoom.course.messaging

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "coderoom.kafka.topics")
data class KafkaTopicsProperties(
    val autogradeJobs: String = "coderoom.autograde.jobs",
    val autogradeResults: String = "coderoom.autograde.results",
)
