package ru.coderoom.course.api.dto

import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Size
import ru.coderoom.course.domain.SubmissionGraderType
import ru.coderoom.course.domain.SubmissionOwnerType
import ru.coderoom.course.domain.SubmissionStatus
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class CreateSubmissionRequest(
    @field:Size(max = 100_000, message = "textAnswer too long")
    val textAnswer: String? = null,
    val attachmentIds: List<UUID> = emptyList(),
)

data class GradeSubmissionRequest(
    @field:DecimalMin(value = "0.00", inclusive = true, message = "score must be between 0 and 100")
    @field:DecimalMax(value = "100.00", inclusive = true, message = "score must be between 0 and 100")
    val score: BigDecimal,

    @field:Size(max = 20_000, message = "comment too long")
    val comment: String? = null,
)

data class SubmissionResponse(
    val submissionId: UUID,
    val courseId: UUID,
    val assignmentId: UUID,
    val ownerType: SubmissionOwnerType,
    val ownerUserId: UUID?,
    val ownerGroupId: UUID?,
    val ownerGroupName: String?,
    val memberUserIds: List<UUID>,
    val textAnswer: String?,
    val attachments: List<AttachmentResponse>,
    val status: SubmissionStatus,
    val score: BigDecimal?,
    val comment: String?,
    val gradedByUserId: UUID?,
    val gradedAt: Instant?,
    val graderType: SubmissionGraderType?,
    val autogradeStatus: String?,
    val externalCheckStatus: String?,
    val submittedAt: Instant,
    val updatedAt: Instant,
)
