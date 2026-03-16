package ru.coderoom.course.content

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "coderoom.content")
data class ContentProperties(
    val baseUrl: String,
    val bucket: String,
)
