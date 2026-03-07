package ru.coderoom.course.identity

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "coderoom.identity")
data class IdentityProperties(
    val baseUrl: String,
)

