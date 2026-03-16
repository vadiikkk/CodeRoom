package ru.coderoom.course.api

import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.coderoom.course.api.dto.AssignmentResponse
import ru.coderoom.course.api.dto.AttachmentResponse
import ru.coderoom.course.api.dto.CodeAttemptLogDownloadResponse
import ru.coderoom.course.api.dto.CodeAttemptResponse
import ru.coderoom.course.api.dto.CodeAssignmentResponse
import ru.coderoom.course.api.dto.CreateCodeAttemptRequest
import ru.coderoom.course.security.RequestUser
import ru.coderoom.course.service.CodeAssignmentAggregate
import ru.coderoom.course.service.CodeAssignmentService
import ru.coderoom.course.service.CodeAttemptAggregate
import ru.coderoom.course.service.CodeAttemptService
import java.util.UUID

@RestController
@RequestMapping("/api/v1")
class CodeAssignmentsController(
    private val codeAssignments: CodeAssignmentService,
    private val codeAttempts: CodeAttemptService,
) {
    @PostMapping("/assignments/{assignmentId}/publish")
    fun publish(
        @AuthenticationPrincipal user: RequestUser,
        @PathVariable assignmentId: UUID,
    ): AssignmentResponse =
        toAssignmentResponse(codeAssignments.publishAssignment(assignmentId, user.userId))

    @PostMapping("/assignments/{assignmentId}/code-attempts")
    fun createAttempt(
        @AuthenticationPrincipal user: RequestUser,
        @PathVariable assignmentId: UUID,
        @Valid @RequestBody req: CreateCodeAttemptRequest,
    ): CodeAttemptResponse =
        toAttemptResponse(codeAttempts.createAttempt(assignmentId, user.userId, req))

    @GetMapping("/assignments/{assignmentId}/code-attempts")
    fun listForAssignment(
        @AuthenticationPrincipal user: RequestUser,
        @PathVariable assignmentId: UUID,
    ): List<CodeAttemptResponse> =
        codeAttempts.listAttemptsForAssignment(assignmentId, user.userId).map(::toAttemptResponse)

    @GetMapping("/assignments/{assignmentId}/code-attempts/me")
    fun listMine(
        @AuthenticationPrincipal user: RequestUser,
        @PathVariable assignmentId: UUID,
    ): List<CodeAttemptResponse> =
        codeAttempts.listMyAttempts(assignmentId, user.userId).map(::toAttemptResponse)

    @GetMapping("/code-attempts/{attemptId}")
    fun getAttempt(
        @AuthenticationPrincipal user: RequestUser,
        @PathVariable attemptId: UUID,
    ): CodeAttemptResponse =
        toAttemptResponse(codeAttempts.getAttempt(attemptId, user.userId))

    @PostMapping("/code-attempts/{attemptId}/retry")
    fun retryAttempt(
        @AuthenticationPrincipal user: RequestUser,
        @PathVariable attemptId: UUID,
    ): CodeAttemptResponse =
        toAttemptResponse(codeAttempts.retryAttempt(attemptId, user.userId))

    @GetMapping("/code-attempts/{attemptId}/log")
    fun downloadAttemptLog(
        @AuthenticationPrincipal user: RequestUser,
        @PathVariable attemptId: UUID,
    ): CodeAttemptLogDownloadResponse {
        val log = codeAttempts.presignAttemptLogDownload(attemptId, user.userId)
        return CodeAttemptLogDownloadResponse(
            attemptId = log.attemptId,
            fileName = log.fileName,
            downloadUrl = log.presigned.url,
            method = log.presigned.method,
        )
    }

    private fun toAttemptResponse(aggregate: CodeAttemptAggregate): CodeAttemptResponse =
        CodeAttemptResponse(
            attemptId = aggregate.attempt.attemptId,
            courseId = aggregate.attempt.courseId,
            assignmentId = aggregate.attempt.assignmentId,
            studentUserId = aggregate.attempt.studentUserId,
            language = aggregate.language,
            attemptNumber = aggregate.attempt.attemptNumber,
            pullRequestUrl = aggregate.attempt.pullRequestUrl,
            pullRequestNumber = aggregate.attempt.pullRequestNumber,
            pullRequestHeadSha = aggregate.attempt.pullRequestHeadSha,
            repositoryFullName = aggregate.attempt.repositoryFullName,
            status = aggregate.attempt.status,
            score = aggregate.attempt.score,
            comment = aggregate.attempt.comment,
            resultSummary = aggregate.attempt.resultSummary,
            logObjectKey = aggregate.attempt.logObjectKey,
            exitCode = aggregate.attempt.exitCode,
            testsPassed = aggregate.attempt.testsPassed,
            testsTotal = aggregate.attempt.testsTotal,
            scoringMode = aggregate.attempt.scoringMode,
            queuedAt = aggregate.attempt.queuedAt,
            startedAt = aggregate.attempt.startedAt,
            finishedAt = aggregate.attempt.finishedAt,
            createdAt = aggregate.attempt.createdAt,
            updatedAt = aggregate.attempt.updatedAt,
        )
    
    private fun toAssignmentResponse(aggregate: CodeAssignmentAggregate): AssignmentResponse =
        AssignmentResponse(
            assignmentId = aggregate.assignment.assignment.assignmentId,
            courseId = aggregate.assignment.assignment.courseId,
            itemId = aggregate.assignment.item.itemId,
            title = aggregate.assignment.assignment.title,
            description = aggregate.assignment.assignment.description,
            assignmentType = aggregate.assignment.assignment.assignmentType,
            workType = aggregate.assignment.assignment.workType,
            deadlineAt = aggregate.assignment.assignment.deadlineAt,
            weight = aggregate.assignment.assignment.weight,
            blockId = aggregate.assignment.item.blockId,
            position = aggregate.assignment.item.position,
            isVisible = aggregate.assignment.item.isVisible,
            attachments = aggregate.assignment.attachments.map(::toAttachmentResponse),
            code = CodeAssignmentResponse(
                language = aggregate.codeAssignment.language,
                repositoryName = aggregate.codeAssignment.repositoryName,
                repositoryFullName = aggregate.codeAssignment.repositoryFullName,
                repositoryUrl = aggregate.codeAssignment.repositoryUrl,
                defaultBranch = aggregate.codeAssignment.defaultBranch,
                maxAttempts = aggregate.codeAssignment.maxAttempts,
                repositoryPrivate = aggregate.codeAssignment.repositoryPrivate,
                publishedAt = aggregate.codeAssignment.publishedAt,
                starterConfig = aggregate.codeAssignment.starterConfig,
                privateTestsAttachment = aggregate.privateTestsAttachment?.let(::toAttachmentResponse),
            ),
            createdAt = aggregate.assignment.assignment.createdAt,
            updatedAt = aggregate.assignment.assignment.updatedAt,
        )

    private fun toAttachmentResponse(entity: ru.coderoom.course.domain.AttachmentEntity): AttachmentResponse =
        AttachmentResponse(
            attachmentId = entity.attachmentId,
            courseId = entity.courseId,
            fileName = entity.originalFilename,
            contentType = entity.contentType,
            fileSize = entity.fileSize,
            createdAt = entity.createdAt,
        )
}
