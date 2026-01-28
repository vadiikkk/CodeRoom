package ru.coderoom.course.security

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "coderoom.jwt")
data class JwtProperties(
    val secret: String,
)
