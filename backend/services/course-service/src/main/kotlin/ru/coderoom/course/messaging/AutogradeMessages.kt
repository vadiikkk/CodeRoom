package ru.coderoom.course.messaging

import ru.coderoom.course.domain.CodeLanguage
import ru.coderoom.course.domain.CodeSubmissionAttemptStatus
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class AttachedObjectPayload(
    val bucket: String,
    val objectKey: String,
    val fileName: String,
)

data class AutogradeRequestedMessage(
    val commandId: UUID,
    val courseId: UUID,
    val assignmentId: UUID,
    val attemptId: UUID,
    val studentUserId: UUID,
    val language: CodeLanguage,
    val repositoryFullName: String,
    val repositoryCloneUrl: String,
    val defaultBranch: String,
    val pullRequestUrl: String,
    val pullRequestNumber: Int,
    val pullRequestHeadSha: String?,
    val configSnapshot: String,
    val privateTests: AttachedObjectPayload?,
    val queuedAt: Instant,
)

data class AutogradeFinishedMessage(
    val attemptId: UUID,
    val status: CodeSubmissionAttemptStatus,
    val score: BigDecimal?,
    val comment: String?,
    val resultSummary: String?,
    val logObjectKey: String?,
    val exitCode: Int?,
    val testsPassed: Int?,
    val testsTotal: Int?,
    val scoringMode: String?,
    val startedAt: Instant?,
    val finishedAt: Instant?,
)
