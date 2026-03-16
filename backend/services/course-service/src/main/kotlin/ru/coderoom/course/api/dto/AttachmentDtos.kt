package ru.coderoom.course.api.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class PresignUploadAttachmentRequest(
    @field:NotNull(message = "courseId is required")
    val courseId: UUID,

    @field:NotBlank(message = "fileName is required")
    @field:Size(max = 255, message = "fileName too long")
    val fileName: String,

    @field:Size(max = 255, message = "contentType too long")
    val contentType: String? = null,

    @field:PositiveOrZero(message = "fileSize must be positive or zero")
    val fileSize: Long,
)

data class PresignUploadAttachmentResponse(
    val attachmentId: UUID,
    val courseId: UUID,
    val fileName: String,
    val contentType: String?,
    val fileSize: Long,
    val uploadUrl: String,
    val method: String,
    val createdAt: Instant,
)

data class PresignDownloadAttachmentRequest(
    @field:NotNull(message = "attachmentId is required")
    val attachmentId: UUID,
)

data class PresignDownloadAttachmentResponse(
    val attachmentId: UUID,
    val fileName: String,
    val contentType: String?,
    val fileSize: Long,
    val downloadUrl: String,
    val method: String,
)

data class AttachmentResponse(
    val attachmentId: UUID,
    val courseId: UUID,
    val fileName: String,
    val contentType: String?,
    val fileSize: Long,
    val createdAt: Instant,
)
