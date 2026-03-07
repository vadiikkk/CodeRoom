package ru.coderoom.content.storage

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "coderoom.storage")
data class StorageProperties(
    val internalEndpoint: String,
    val publicEndpoint: String,
    val accessKey: String,
    val secretKey: String,
    val bucket: String,
    val region: String,
    val uploadExpiryMinutes: Int,
    val downloadExpiryMinutes: Int,
)
