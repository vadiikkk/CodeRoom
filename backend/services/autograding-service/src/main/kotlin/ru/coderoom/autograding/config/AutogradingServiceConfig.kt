package ru.coderoom.autograding.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka

@Configuration
@EnableKafka
@EnableConfigurationProperties(KafkaTopicsProperties::class)
class AutogradingServiceConfig
