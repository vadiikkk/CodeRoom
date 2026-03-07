package ru.coderoom.runner.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka

@Configuration
@EnableKafka
@EnableConfigurationProperties(
    KafkaTopicsProperties::class,
    ContentProperties::class,
    RunnerProperties::class,
)
class RunnerServiceConfig
