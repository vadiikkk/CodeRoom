package ru.coderoom.course.github

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "coderoom.github")
data class GithubProperties(
    val apiBaseUrl: String = "https://api.github.com",
    val cloneBaseUrl: String = "https://github.com",
)
