package ru.coderoom.course.messaging

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class AutogradePublisher(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper,
    private val topics: KafkaTopicsProperties,
) {
    fun publishRequested(message: AutogradeRequestedMessage) {
        kafkaTemplate.send(
            topics.autogradeJobs,
            message.attemptId.toString(),
            objectMapper.writeValueAsString(message),
        )
    }
}
