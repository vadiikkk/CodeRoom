package ru.coderoom.identity.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "coderoom.bootstrap.root")
data class BootstrapProperties(
    val email: String = "",
    val password: String = "",
)
