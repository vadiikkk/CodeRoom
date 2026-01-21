package ru.coderoom.identity.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import ru.coderoom.identity.auth.JwtProperties

@Configuration
@EnableConfigurationProperties(JwtProperties::class, BootstrapProperties::class)
class IdentityServiceConfig
