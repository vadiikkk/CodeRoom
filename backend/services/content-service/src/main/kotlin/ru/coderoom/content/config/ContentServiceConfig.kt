package ru.coderoom.content.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import ru.coderoom.content.storage.StorageProperties

@Configuration
@EnableConfigurationProperties(StorageProperties::class)
class ContentServiceConfig
