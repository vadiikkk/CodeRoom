package ru.coderoom.course.api

import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import ru.coderoom.course.api.dto.AttachmentResponse
import ru.coderoom.course.api.dto.CreateSubmissionRequest
import ru.coderoom.course.api.dto.GradeSubmissionRequest
import ru.coderoom.course.api.dto.SubmissionResponse
import ru.coderoom.course.domain.AttachmentEntity
import ru.coderoom.course.security.RequestUser
import ru.coderoom.course.service.SubmissionAggregate
import ru.coderoom.course.service.SubmissionService
import java.util.UUID

@RestController
class SubmissionsController(
    private val submissions: SubmissionService,
) {
    @PostMapping("/api/v1/assignments/{assignmentId}/submissions")
    fun createOrUpdateMine(
        @AuthenticationPrincipal user: RequestUser,
        @PathVariable assignmentId: UUID,
        @Valid @RequestBody req: CreateSubmissionRequest,
    ): SubmissionResponse =
        toResponse(
            submissions.createOrUpdateMySubmission(
                assignmentId = assignmentId,
                userId = user.userId,
                textAnswer = req.textAnswer,
                attachmentIds = req.attachmentIds,
            ),
        )

    @GetMapping("/api/v1/assignments/{assignmentId}/submissions")
    fun listForAssignment(
        @AuthenticationPrincipal user: RequestUser,
        @PathVariable assignmentId: UUID,
    ): List<SubmissionResponse> =
        submissions.listSubmissionsForAssignment(assignmentId, user.userId).map(::toResponse)

    @GetMapping("/api/v1/assignments/{assignmentId}/submissions/me")
    fun mySubmission(
        @AuthenticationPrincipal user: RequestUser,
        @PathVariable assignmentId: UUID,
    ): SubmissionResponse =
        toResponse(submissions.getMySubmissionForAssignment(assignmentId, user.userId))

    @GetMapping("/api/v1/submissions/{submissionId}")
    fun getSubmission(
        @AuthenticationPrincipal user: RequestUser,
        @PathVariable submissionId: UUID,
    ): SubmissionResponse =
        toResponse(submissions.getSubmission(submissionId, user.userId))

    @PutMapping("/api/v1/submissions/{submissionId}/grade")
    fun gradeSubmission(
        @AuthenticationPrincipal user: RequestUser,
        @PathVariable submissionId: UUID,
        @Valid @RequestBody req: GradeSubmissionRequest,
    ): SubmissionResponse =
        toResponse(
            submissions.gradeSubmission(
                submissionId = submissionId,
                userId = user.userId,
                score = req.score,
                comment = req.comment?.trim()?.ifBlank { null },
            ),
        )

    private fun toResponse(aggregate: SubmissionAggregate): SubmissionResponse =
        SubmissionResponse(
            submissionId = aggregate.submission.submissionId,
            courseId = aggregate.submission.courseId,
            assignmentId = aggregate.submission.assignmentId,
            ownerType = aggregate.submission.ownerType,
            ownerUserId = aggregate.submission.ownerUserId,
            ownerGroupId = aggregate.submission.ownerGroupId,
            ownerGroupName = aggregate.group?.name,
            memberUserIds = aggregate.memberUserIds,
            textAnswer = aggregate.submission.textAnswer,
            attachments = aggregate.attachments.map(::toAttachmentResponse),
            status = aggregate.submission.status,
            score = aggregate.submission.score,
            comment = aggregate.submission.comment,
            gradedByUserId = aggregate.submission.gradedByUserId,
            gradedAt = aggregate.submission.gradedAt,
            graderType = aggregate.submission.graderType,
            autogradeStatus = aggregate.submission.autogradeStatus,
            externalCheckStatus = aggregate.submission.externalCheckStatus,
            submittedAt = aggregate.submission.submittedAt,
            updatedAt = aggregate.submission.updatedAt,
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
