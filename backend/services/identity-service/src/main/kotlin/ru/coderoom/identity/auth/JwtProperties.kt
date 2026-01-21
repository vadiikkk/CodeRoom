package ru.coderoom.identity.auth

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "coderoom.jwt")
data class JwtProperties(
    val secret: String,
    val accessTokenTtlSeconds: Long = 60L * 60L, // 1h
    val refreshTokenTtlSeconds: Long = 60L * 60L * 24L * 30L, // 30d
)
