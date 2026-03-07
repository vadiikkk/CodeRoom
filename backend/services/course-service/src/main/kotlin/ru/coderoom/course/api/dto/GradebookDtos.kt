package ru.coderoom.course.api.dto

import ru.coderoom.course.domain.AssignmentType
import ru.coderoom.course.domain.SubmissionOwnerType
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class GradebookAssignmentResponse(
    val assignmentId: UUID,
    val title: String,
    val assignmentType: AssignmentType,
    val weight: BigDecimal,
)

data class GradebookEntryResponse(
    val assignmentId: UUID,
    val submissionId: UUID?,
    val codeAttemptId: UUID?,
    val status: String?,
    val score: BigDecimal?,
    val weightedScore: BigDecimal?,
    val comment: String?,
    val ownerType: SubmissionOwnerType?,
    val ownerGroupId: UUID?,
    val ownerGroupName: String?,
    val submittedAt: Instant?,
    val gradedAt: Instant?,
)

data class GradebookStudentRowResponse(
    val userId: UUID,
    val entries: List<GradebookEntryResponse>,
    val totalWeightedScore: BigDecimal,
)

data class CourseGradebookResponse(
    val assignments: List<GradebookAssignmentResponse>,
    val rows: List<GradebookStudentRowResponse>,
)

data class MyGradebookResponse(
    val userId: UUID,
    val assignments: List<GradebookAssignmentResponse>,
    val entries: List<GradebookEntryResponse>,
    val totalWeightedScore: BigDecimal,
)
