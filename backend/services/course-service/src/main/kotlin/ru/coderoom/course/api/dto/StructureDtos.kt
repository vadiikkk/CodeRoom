package ru.coderoom.course.api.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID
import ru.coderoom.course.domain.CourseItemType

data class CreateBlockRequest(
    @field:NotBlank(message = "title is required")
    @field:Size(max = 200, message = "title too long")
    val title: String,
    val position: Int,
    val isVisible: Boolean = true,
)

data class UpdateBlockRequest(
    @field:Size(max = 200, message = "title too long")
    val title: String? = null,
    val position: Int? = null,
    val isVisible: Boolean? = null,
)

data class BlockResponse(
    val blockId: UUID,
    val courseId: UUID,
    val title: String,
    val position: Int,
    val isVisible: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class CreateItemRequest(
    val blockId: UUID? = null,

    @field:NotNull(message = "itemType is required")
    val itemType: CourseItemType,

    @field:NotNull(message = "refId is required")
    val refId: UUID,

    val position: Int,
    val isVisible: Boolean = true,
)

data class UpdateItemRequest(
    val blockId: UUID? = null,
    val itemType: CourseItemType? = null,
    val refId: UUID? = null,
    val position: Int? = null,
    val isVisible: Boolean? = null,
)

data class ItemResponse(
    val itemId: UUID,
    val courseId: UUID,
    val blockId: UUID?,
    val itemType: CourseItemType,
    val refId: UUID,
    val position: Int,
    val isVisible: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class CourseStructureResponse(
    val blocks: List<BlockWithItems>,
    val rootItems: List<ItemResponse>,
)

data class BlockWithItems(
    val block: BlockResponse,
    val items: List<ItemResponse>,
)
