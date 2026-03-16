package ru.coderoom.course.api.dto

import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import ru.coderoom.course.domain.AssignmentType
import ru.coderoom.course.domain.AssignmentWorkType
import ru.coderoom.course.domain.CodeLanguage
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class CreateAssignmentRequest(
    @field:NotBlank(message = "title is required")
    @field:Size(max = 200, message = "title too long")
    val title: String,

    @field:Size(max = 20_000, message = "description too long")
    val description: String? = null,

    @field:NotNull(message = "assignmentType is required")
    val assignmentType: AssignmentType,

    val workType: AssignmentWorkType = AssignmentWorkType.INDIVIDUAL,
    val deadlineAt: Instant? = null,

    @field:DecimalMin(value = "0.00", inclusive = true, message = "weight must be between 0 and 1")
    @field:DecimalMax(value = "1.00", inclusive = true, message = "weight must be between 0 and 1")
    val weight: BigDecimal = BigDecimal.ONE,

    val blockId: UUID? = null,
    val position: Int,
    val isVisible: Boolean = true,
    val attachmentIds: List<UUID> = emptyList(),
    val code: CreateCodeAssignmentRequest? = null,
)

data class UpdateAssignmentRequest(
    @field:Size(max = 200, message = "title too long")
    val title: String? = null,

    @field:Size(max = 20_000, message = "description too long")
    val description: String? = null,

    val assignmentType: AssignmentType? = null,
    val workType: AssignmentWorkType? = null,
    val deadlineAt: Instant? = null,
    val clearDeadline: Boolean = false,

    @field:DecimalMin(value = "0.00", inclusive = true, message = "weight must be between 0 and 1")
    @field:DecimalMax(value = "1.00", inclusive = true, message = "weight must be between 0 and 1")
    val weight: BigDecimal? = null,

    val blockId: UUID? = null,
    val clearBlock: Boolean = false,
    val position: Int? = null,
    val isVisible: Boolean? = null,
    val attachmentIds: List<UUID>? = null,
    val code: UpdateCodeAssignmentRequest? = null,
)

data class AssignmentResponse(
    val assignmentId: UUID,
    val courseId: UUID,
    val itemId: UUID,
    val title: String,
    val description: String?,
    val assignmentType: AssignmentType,
    val workType: AssignmentWorkType,
    val deadlineAt: Instant?,
    val weight: BigDecimal,
    val blockId: UUID?,
    val position: Int,
    val isVisible: Boolean,
    val attachments: List<AttachmentResponse>,
    val code: CodeAssignmentResponse? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class CreateCodeAssignmentRequest(
    @field:NotBlank(message = "code.repositoryName is required")
    @field:Size(max = 120, message = "code.repositoryName too long")
    val repositoryName: String,
    val repositoryDescription: String? = null,
    @field:NotNull(message = "code.language is required")
    val language: CodeLanguage,
    @field:Positive(message = "code.maxAttempts must be positive")
    val maxAttempts: Int,
    val privateTestsAttachmentId: UUID? = null,
    @field:Size(max = 10_000, message = "code.githubPat too long")
    val githubPat: String? = null,
)

data class UpdateCodeAssignmentRequest(
    @field:Positive(message = "code.maxAttempts must be positive")
    val maxAttempts: Int? = null,
    val privateTestsAttachmentId: UUID? = null,
    val clearPrivateTestsAttachment: Boolean = false,
)

data class CodeAssignmentResponse(
    val language: CodeLanguage,
    val repositoryName: String,
    val repositoryFullName: String,
    val repositoryUrl: String,
    val defaultBranch: String,
    val maxAttempts: Int,
    val repositoryPrivate: Boolean,
    val publishedAt: Instant?,
    val starterConfig: String,
    val privateTestsAttachment: AttachmentResponse?,
)
