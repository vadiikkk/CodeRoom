package ru.coderoom.course.messaging

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import ru.coderoom.course.service.CodeAttemptService

@Component
class AutogradeResultListener(
    private val objectMapper: ObjectMapper,
    private val codeAttempts: CodeAttemptService,
) {
    @KafkaListener(topics = ["\${coderoom.kafka.topics.autograde-results}"], groupId = "course-service")
    fun onMessage(payload: String) {
        val message = objectMapper.readValue(payload, AutogradeFinishedMessage::class.java)
        codeAttempts.applyAutogradeResult(message)
    }
}
