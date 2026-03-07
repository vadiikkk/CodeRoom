package ru.coderoom.autograding.messaging

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import ru.coderoom.autograding.service.AutogradingService

@Component
class KafkaListeners(
    private val objectMapper: ObjectMapper,
    private val autograding: AutogradingService,
) {
    @KafkaListener(topics = ["\${coderoom.kafka.topics.autograde-jobs}"], groupId = "autograding-service")
    fun onAutogradeRequested(payload: String) {
        autograding.enqueue(objectMapper.readValue(payload, AutogradeRequestedMessage::class.java))
    }

    @KafkaListener(topics = ["\${coderoom.kafka.topics.runner-results}"], groupId = "autograding-service")
    fun onRunnerFinished(payload: String) {
        autograding.complete(objectMapper.readValue(payload, RunnerFinishedMessage::class.java))
    }
}
