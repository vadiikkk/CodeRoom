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
import ru.coderoom.course.api.dto.BlockResponse
import ru.coderoom.course.api.dto.BlockWithItems
import ru.coderoom.course.api.dto.CourseStructureResponse
import ru.coderoom.course.api.dto.CreateBlockRequest
import ru.coderoom.course.api.dto.CreateItemRequest
import ru.coderoom.course.api.dto.ItemResponse
import ru.coderoom.course.api.dto.UpdateBlockRequest
import ru.coderoom.course.api.dto.UpdateItemRequest
import ru.coderoom.course.domain.CourseBlockEntity
import ru.coderoom.course.domain.CourseItemEntity
import ru.coderoom.course.security.RequestUser
import ru.coderoom.course.service.CourseAppService
import java.util.UUID

@RestController
@RequestMapping("/api/v1/courses/{courseId}")
class CourseStructureController(
    private val app: CourseAppService,
) {
    @GetMapping("/structure")
    fun structure(
        @AuthenticationPrincipal user: RequestUser,
        @PathVariable courseId: UUID,
    ): CourseStructureResponse {
        val structure = app.getStructure(courseId, user.userId)
        val blocks = structure.blocks.sortedBy { it.position }
        val items = structure.items.sortedBy { it.position }

        val itemsByBlock = items.filter { it.blockId != null }.groupBy { it.blockId!! }
        val rootItems = items.filter { it.blockId == null }.map(::toItemResponse)

        return CourseStructureResponse(
            blocks = blocks.map { block ->
                BlockWithItems(
                    block = toBlockResponse(block),
                    items = itemsByBlock[block.blockId].orEmpty().map(::toItemResponse),
                )
            },
            rootItems = rootItems,
        )
    }

    @PostMapping("/blocks")
    fun createBlock(
        @AuthenticationPrincipal user: RequestUser,
        @PathVariable courseId: UUID,
        @Valid @RequestBody req: CreateBlockRequest,
    ): BlockResponse =
        toBlockResponse(
            app.createBlock(
                courseId = courseId,
                userId = user.userId,
                title = req.title.trim(),
                position = req.position,
                isVisible = req.isVisible,
            ),
        )

    @PutMapping("/blocks/{blockId}")
    fun updateBlock(
        @AuthenticationPrincipal user: RequestUser,
        @PathVariable courseId: UUID,
        @PathVariable blockId: UUID,
        @Valid @RequestBody req: UpdateBlockRequest,
    ): BlockResponse =
        toBlockResponse(
            app.updateBlock(
                courseId = courseId,
                userId = user.userId,
                blockId = blockId,
                title = req.title?.trim(),
                position = req.position,
                isVisible = req.isVisible,
            ),
        )

    @DeleteMapping("/blocks/{blockId}")
    fun deleteBlock(
        @AuthenticationPrincipal user: RequestUser,
        @PathVariable courseId: UUID,
        @PathVariable blockId: UUID,
    ) {
        app.deleteBlock(courseId = courseId, userId = user.userId, blockId = blockId)
    }

    @PostMapping("/items")
    fun createItem(
        @AuthenticationPrincipal user: RequestUser,
        @PathVariable courseId: UUID,
        @Valid @RequestBody req: CreateItemRequest,
    ): ItemResponse =
        toItemResponse(
            app.createItem(
                courseId = courseId,
                userId = user.userId,
                blockId = req.blockId,
                itemType = req.itemType,
                refId = req.refId,
                position = req.position,
                isVisible = req.isVisible,
            ),
        )

    @PutMapping("/items/{itemId}")
    fun updateItem(
        @AuthenticationPrincipal user: RequestUser,
        @PathVariable courseId: UUID,
        @PathVariable itemId: UUID,
        @Valid @RequestBody req: UpdateItemRequest,
    ): ItemResponse =
        toItemResponse(
            app.updateItem(
                courseId = courseId,
                userId = user.userId,
                itemId = itemId,
                blockId = req.blockId,
                itemType = req.itemType,
                refId = req.refId,
                position = req.position,
                isVisible = req.isVisible,
            ),
        )

    @DeleteMapping("/items/{itemId}")
    fun deleteItem(
        @AuthenticationPrincipal user: RequestUser,
        @PathVariable courseId: UUID,
        @PathVariable itemId: UUID,
    ) {
        app.deleteItem(courseId = courseId, userId = user.userId, itemId = itemId)
    }

    private fun toBlockResponse(e: CourseBlockEntity): BlockResponse =
        BlockResponse(
            blockId = e.blockId,
            courseId = e.courseId,
            title = e.title,
            position = e.position,
            isVisible = e.isVisible,
            createdAt = e.createdAt,
            updatedAt = e.updatedAt,
        )

    private fun toItemResponse(e: CourseItemEntity): ItemResponse =
        ItemResponse(
            itemId = e.itemId,
            courseId = e.courseId,
            blockId = e.blockId,
            itemType = e.itemType,
            refId = e.refId,
            position = e.position,
            isVisible = e.isVisible,
            createdAt = e.createdAt,
            updatedAt = e.updatedAt,
        )
}
