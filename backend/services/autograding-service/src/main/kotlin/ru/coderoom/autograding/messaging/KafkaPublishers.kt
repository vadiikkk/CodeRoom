package ru.coderoom.autograding.messaging

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import ru.coderoom.autograding.config.KafkaTopicsProperties

@Component
class KafkaPublishers(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper,
    private val topics: KafkaTopicsProperties,
) {
    fun publishRunnerJob(message: RunTestRunMessage) {
        kafkaTemplate.send(topics.runnerJobs, message.testRunId.toString(), objectMapper.writeValueAsString(message))
    }

    fun publishAutogradeResult(message: AutogradeFinishedMessage) {
        kafkaTemplate.send(topics.autogradeResults, message.attemptId.toString(), objectMapper.writeValueAsString(message))
    }
}
