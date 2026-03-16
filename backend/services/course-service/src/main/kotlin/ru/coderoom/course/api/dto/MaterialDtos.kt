package ru.coderoom.course.api.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class CreateMaterialRequest(
    @field:NotBlank(message = "title is required")
    @field:Size(max = 200, message = "title too long")
    val title: String,

    @field:Size(max = 10_000, message = "description too long")
    val description: String? = null,

    @field:Size(max = 100_000, message = "body too long")
    val body: String? = null,

    val blockId: UUID? = null,
    val position: Int,
    val isVisible: Boolean = true,
    val attachmentIds: List<UUID> = emptyList(),
)

data class UpdateMaterialRequest(
    @field:Size(max = 200, message = "title too long")
    val title: String? = null,

    @field:Size(max = 10_000, message = "description too long")
    val description: String? = null,

    @field:Size(max = 100_000, message = "body too long")
    val body: String? = null,

    val blockId: UUID? = null,
    val clearBlock: Boolean = false,
    val position: Int? = null,
    val isVisible: Boolean? = null,
    val attachmentIds: List<UUID>? = null,
)

data class MaterialResponse(
    val materialId: UUID,
    val courseId: UUID,
    val itemId: UUID,
    val title: String,
    val description: String?,
    val body: String?,
    val blockId: UUID?,
    val position: Int,
    val isVisible: Boolean,
    val attachments: List<AttachmentResponse>,
    val createdAt: Instant,
    val updatedAt: Instant,
)
