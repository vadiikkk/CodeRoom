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
import ru.coderoom.course.api.dto.AssignmentResponse
import ru.coderoom.course.api.dto.AttachmentResponse
import ru.coderoom.course.api.dto.CodeAssignmentResponse
import ru.coderoom.course.api.dto.CreateAssignmentRequest
import ru.coderoom.course.api.dto.UpdateAssignmentRequest
import ru.coderoom.course.domain.AssignmentType
import ru.coderoom.course.domain.AttachmentEntity
import ru.coderoom.course.security.RequestUser
import ru.coderoom.course.service.AssignmentAggregate
import ru.coderoom.course.service.CodeAssignmentExtras
import ru.coderoom.course.service.CodeAssignmentService
import ru.coderoom.course.service.CourseContentService
import java.util.UUID

@RestController
@RequestMapping("/api/v1")
class AssignmentsController(
    private val content: CourseContentService,
    private val codeAssignments: CodeAssignmentService,
) {
    @PostMapping("/courses/{courseId}/assignments")
    fun create(
        @AuthenticationPrincipal user: RequestUser,
        @PathVariable courseId: UUID,
        @Valid @RequestBody req: CreateAssignmentRequest,
    ): AssignmentResponse =
        if (req.assignmentType == AssignmentType.CODE) {
            val code = req.code ?: throw IllegalArgumentException("code settings are required for CODE assignment")
            toResponse(
                codeAssignments.createAssignment(
                    courseId = courseId,
                    userId = user.userId,
                    title = req.title.trim(),
                    description = req.description?.trim()?.ifBlank { null },
                    workType = req.workType,
                    deadlineAt = req.deadlineAt,
                    weight = req.weight,
                    blockId = req.blockId,
                    position = req.position,
                    isVisible = req.isVisible,
                    attachmentIds = req.attachmentIds,
                    code = code,
                ),
            )
        } else {
            toResponse(
                content.createAssignment(
                    courseId = courseId,
                    userId = user.userId,
                    title = req.title.trim(),
                    description = req.description?.trim()?.ifBlank { null },
                    assignmentType = req.assignmentType,
                    workType = req.workType,
                    deadlineAt = req.deadlineAt,
                    weight = req.weight,
                    blockId = req.blockId,
                    position = req.position,
                    isVisible = req.isVisible,
                    attachmentIds = req.attachmentIds,
                ),
                code = null,
            )
        }

    @GetMapping("/courses/{courseId}/assignments")
    fun list(
        @AuthenticationPrincipal user: RequestUser,
        @PathVariable courseId: UUID,
    ): List<AssignmentResponse> =
        content.listAssignments(courseId = courseId, userId = user.userId)
            .map { aggregate ->
                toResponse(aggregate, codeAssignments.findCodeAssignmentExtras(aggregate.assignment.assignmentId))
            }

    @GetMapping("/assignments/{assignmentId}")
    fun get(
        @AuthenticationPrincipal user: RequestUser,
        @PathVariable assignmentId: UUID,
    ): AssignmentResponse =
        toResponse(
            content.getAssignment(assignmentId = assignmentId, userId = user.userId),
            codeAssignments.findCodeAssignmentExtras(assignmentId),
        )

    @PutMapping("/assignments/{assignmentId}")
    fun update(
        @AuthenticationPrincipal user: RequestUser,
        @PathVariable assignmentId: UUID,
        @Valid @RequestBody req: UpdateAssignmentRequest,
    ): AssignmentResponse {
        val current = content.getAssignment(assignmentId = assignmentId, userId = user.userId)
        val isCode = current.assignment.assignmentType == AssignmentType.CODE || req.assignmentType == AssignmentType.CODE || req.code != null
        return if (isCode) {
            toResponse(
                codeAssignments.updateCodeAssignment(
                    assignmentId = assignmentId,
                    userId = user.userId,
                    title = req.title?.trim(),
                    description = req.description?.trim()?.ifBlank { null },
                    deadlineAt = req.deadlineAt,
                    clearDeadline = req.clearDeadline,
                    weight = req.weight,
                    blockId = req.blockId,
                    clearBlock = req.clearBlock,
                    position = req.position,
                    isVisible = req.isVisible,
                    attachmentIds = req.attachmentIds,
                    code = req.code,
                ),
            )
        } else {
            toResponse(
                content.updateAssignment(
                    assignmentId = assignmentId,
                    userId = user.userId,
                    title = req.title?.trim(),
                    description = req.description?.trim()?.ifBlank { null },
                    assignmentType = req.assignmentType,
                    workType = req.workType,
                    deadlineAt = req.deadlineAt,
                    clearDeadline = req.clearDeadline,
                    weight = req.weight,
                    blockId = req.blockId,
                    clearBlock = req.clearBlock,
                    position = req.position,
                    isVisible = req.isVisible,
                    attachmentIds = req.attachmentIds,
                ),
                code = null,
            )
        }
    }

    @DeleteMapping("/assignments/{assignmentId}")
    fun delete(
        @AuthenticationPrincipal user: RequestUser,
        @PathVariable assignmentId: UUID,
    ) {
        content.deleteAssignment(assignmentId = assignmentId, userId = user.userId)
    }

    private fun toResponse(
        aggregate: AssignmentAggregate,
        code: CodeAssignmentExtras?,
    ): AssignmentResponse =
        AssignmentResponse(
            assignmentId = aggregate.assignment.assignmentId,
            courseId = aggregate.assignment.courseId,
            itemId = aggregate.item.itemId,
            title = aggregate.assignment.title,
            description = aggregate.assignment.description,
            assignmentType = aggregate.assignment.assignmentType,
            workType = aggregate.assignment.workType,
            deadlineAt = aggregate.assignment.deadlineAt,
            weight = aggregate.assignment.weight,
            blockId = aggregate.item.blockId,
            position = aggregate.item.position,
            isVisible = aggregate.item.isVisible,
            attachments = aggregate.attachments.map(::toAttachmentResponse),
            code = code?.let {
                CodeAssignmentResponse(
                    language = it.codeAssignment.language,
                    repositoryName = it.codeAssignment.repositoryName,
                    repositoryFullName = it.codeAssignment.repositoryFullName,
                    repositoryUrl = it.codeAssignment.repositoryUrl,
                    defaultBranch = it.codeAssignment.defaultBranch,
                    maxAttempts = it.codeAssignment.maxAttempts,
                    repositoryPrivate = it.codeAssignment.repositoryPrivate,
                    publishedAt = it.codeAssignment.publishedAt,
                    starterConfig = it.codeAssignment.starterConfig,
                    privateTestsAttachment = it.privateTestsAttachment?.let(::toAttachmentResponse),
                )
            },
            createdAt = aggregate.assignment.createdAt,
            updatedAt = aggregate.assignment.updatedAt,
        )

    private fun toResponse(aggregate: ru.coderoom.course.service.CodeAssignmentAggregate): AssignmentResponse =
        toResponse(
            aggregate.assignment,
            CodeAssignmentExtras(
                codeAssignment = aggregate.codeAssignment,
                privateTestsAttachment = aggregate.privateTestsAttachment,
            ),
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
