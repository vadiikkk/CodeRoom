package ru.coderoom.runner.messaging

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import ru.coderoom.runner.config.KafkaTopicsProperties

@Component
class KafkaPublisher(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper,
    private val topics: KafkaTopicsProperties,
) {
    fun publishResult(message: RunnerFinishedMessage) {
        kafkaTemplate.send(topics.runnerResults, message.testRunId.toString(), objectMapper.writeValueAsString(message))
    }
}
