package ru.coderoom.course.api

import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.coderoom.course.api.dto.AttachmentResponse
import ru.coderoom.course.api.dto.CreateMaterialRequest
import ru.coderoom.course.api.dto.MaterialResponse
import ru.coderoom.course.api.dto.UpdateMaterialRequest
import ru.coderoom.course.domain.AttachmentEntity
import ru.coderoom.course.security.RequestUser
import ru.coderoom.course.service.CourseContentService
import ru.coderoom.course.service.MaterialAggregate
import java.util.UUID

@RestController
class MaterialsController(
    private val content: CourseContentService,
) {
    @PostMapping("/api/v1/courses/{courseId}/materials")
    fun create(
        @AuthenticationPrincipal user: RequestUser,
        @PathVariable courseId: UUID,
        @Valid @RequestBody req: CreateMaterialRequest,
    ): MaterialResponse =
        toResponse(
            content.createMaterial(
                courseId = courseId,
                userId = user.userId,
                title = req.title.trim(),
                description = req.description?.trim()?.ifBlank { null },
                body = req.body?.trim()?.ifBlank { null },
                blockId = req.blockId,
                position = req.position,
                isVisible = req.isVisible,
                attachmentIds = req.attachmentIds,
            ),
        )

    @GetMapping("/api/v1/courses/{courseId}/materials")
    fun list(
        @AuthenticationPrincipal user: RequestUser,
        @PathVariable courseId: UUID,
    ): List<MaterialResponse> =
        content.listMaterials(courseId = courseId, userId = user.userId).map(::toResponse)

    @GetMapping("/api/v1/materials/{materialId}")
    fun get(
        @AuthenticationPrincipal user: RequestUser,
        @PathVariable materialId: UUID,
    ): MaterialResponse =
        toResponse(content.getMaterial(materialId = materialId, userId = user.userId))

    @PutMapping("/api/v1/materials/{materialId}")
    fun update(
        @AuthenticationPrincipal user: RequestUser,
        @PathVariable materialId: UUID,
        @Valid @RequestBody req: UpdateMaterialRequest,
    ): MaterialResponse =
        toResponse(
            content.updateMaterial(
                materialId = materialId,
                userId = user.userId,
                title = req.title?.trim(),
                description = req.description?.trim()?.ifBlank { null },
                body = req.body?.trim()?.ifBlank { null },
                blockId = req.blockId,
                clearBlock = req.clearBlock,
                position = req.position,
                isVisible = req.isVisible,
                attachmentIds = req.attachmentIds,
            ),
        )

    @DeleteMapping("/api/v1/materials/{materialId}")
    fun delete(
        @AuthenticationPrincipal user: RequestUser,
        @PathVariable materialId: UUID,
    ) {
        content.deleteMaterial(materialId = materialId, userId = user.userId)
    }

    private fun toResponse(aggregate: MaterialAggregate): MaterialResponse =
        MaterialResponse(
            materialId = aggregate.material.materialId,
            courseId = aggregate.material.courseId,
            itemId = aggregate.item.itemId,
            title = aggregate.material.title,
            description = aggregate.material.description,
            body = aggregate.material.body,
            blockId = aggregate.item.blockId,
            position = aggregate.item.position,
            isVisible = aggregate.item.isVisible,
            attachments = aggregate.attachments.map(::toAttachmentResponse),
            createdAt = aggregate.material.createdAt,
            updatedAt = aggregate.material.updatedAt,
        )

    private fun toAttachmentResponse(entity: AttachmentEntity): AttachmentResponse =
        AttachmentResponse(
            attachmentId = entity.attachmentId,
            courseId = entity.courseId,
            fileName = entity.originalFilename,
            contentType = entity.contentType,
            fileSize = entity.fileSize,
            createdAt = entity.createdAt,
        )
}
