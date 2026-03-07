package ru.coderoom.course.api.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import ru.coderoom.course.domain.CodeLanguage
import ru.coderoom.course.domain.CodeSubmissionAttemptStatus
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class CreateCodeAttemptRequest(
    @field:NotBlank(message = "pullRequestUrl is required")
    @field:Size(max = 500, message = "pullRequestUrl too long")
    val pullRequestUrl: String,
)

data class CodeAttemptResponse(
    val attemptId: UUID,
    val courseId: UUID,
    val assignmentId: UUID,
    val studentUserId: UUID,
    val language: CodeLanguage,
    val attemptNumber: Int,
    val pullRequestUrl: String,
    val pullRequestNumber: Int,
    val pullRequestHeadSha: String?,
    val repositoryFullName: String,
    val status: CodeSubmissionAttemptStatus,
    val score: BigDecimal?,
    val comment: String?,
    val resultSummary: String?,
    val logObjectKey: String?,
    val exitCode: Int?,
    val testsPassed: Int?,
    val testsTotal: Int?,
    val scoringMode: String?,
    val queuedAt: Instant,
    val startedAt: Instant?,
    val finishedAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class CodeAttemptLogDownloadResponse(
    val attemptId: UUID,
    val fileName: String,
    val downloadUrl: String,
    val method: String,
)
