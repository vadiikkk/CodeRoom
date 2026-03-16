package ru.coderoom.course.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import ru.coderoom.course.content.ContentProperties
import ru.coderoom.course.github.GithubProperties
import ru.coderoom.course.messaging.KafkaTopicsProperties
import ru.coderoom.course.security.JwtProperties
import ru.coderoom.course.security.CourseProperties
import ru.coderoom.course.identity.IdentityProperties

@Configuration
@EnableKafka
@EnableConfigurationProperties(
    JwtProperties::class,
    CourseProperties::class,
    IdentityProperties::class,
    ContentProperties::class,
    GithubProperties::class,
    KafkaTopicsProperties::class,
)
class CourseServiceConfig
