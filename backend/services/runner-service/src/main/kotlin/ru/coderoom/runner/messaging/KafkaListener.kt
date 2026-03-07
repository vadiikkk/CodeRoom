package ru.coderoom.runner.messaging

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import ru.coderoom.runner.service.RunnerService

@Component
class RunnerKafkaListener(
    private val objectMapper: ObjectMapper,
    private val runner: RunnerService,
    private val publisher: KafkaPublisher,
) {
    @KafkaListener(topics = ["\${coderoom.kafka.topics.runner-jobs}"], groupId = "runner-service")
    fun onRunnerJob(payload: String) {
        val message = objectMapper.readValue(payload, RunTestRunMessage::class.java)
        publisher.publishResult(runner.run(message))
    }
}
