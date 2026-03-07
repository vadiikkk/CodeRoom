package ru.coderoom.content.api.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class PresignUploadObjectRequest(
    @field:NotBlank(message = "objectKey is required")
    @field:Size(max = 500, message = "objectKey too long")
    val objectKey: String,

    @field:Size(max = 255, message = "contentType too long")
    val contentType: String? = null,
)

data class PresignDownloadObjectRequest(
    @field:NotBlank(message = "objectKey is required")
    @field:Size(max = 500, message = "objectKey too long")
    val objectKey: String,

    @field:NotBlank(message = "fileName is required")
    @field:Size(max = 255, message = "fileName too long")
    val fileName: String,
)

data class DownloadObjectRequest(
    @field:NotBlank(message = "objectKey is required")
    @field:Size(max = 500, message = "objectKey too long")
    val objectKey: String,
)

data class DeleteObjectRequest(
    @field:NotBlank(message = "objectKey is required")
    @field:Size(max = 500, message = "objectKey too long")
    val objectKey: String,
)

data class PresignedUrlResponse(
    val url: String,
    val method: String,
)
