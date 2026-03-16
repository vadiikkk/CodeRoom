package ru.coderoom.course.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "code_submission_attempts")
class CodeSubmissionAttemptEntity(
    @Id
    @Column(name = "attempt_id", nullable = false)
    val attemptId: UUID,

    @Column(name = "course_id", nullable = false)
    val courseId: UUID,

    @Column(name = "assignment_id", nullable = false)
    val assignmentId: UUID,

    @Column(name = "student_user_id", nullable = false)
    val studentUserId: UUID,

    @Column(name = "attempt_number", nullable = false)
    val attemptNumber: Int,

    @Column(name = "pull_request_url", nullable = false, length = 500)
    var pullRequestUrl: String,

    @Column(name = "pull_request_number", nullable = false)
    var pullRequestNumber: Int,

    @Column(name = "pull_request_head_sha", length = 64)
    var pullRequestHeadSha: String? = null,

    @Column(name = "repository_full_name", nullable = false, length = 255)
    var repositoryFullName: String,

    @Column(name = "config_snapshot", nullable = false, columnDefinition = "text")
    var configSnapshot: String,

    @Column(name = "private_tests_attachment_id")
    var privateTestsAttachmentId: UUID? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    var status: CodeSubmissionAttemptStatus,

    @Column(name = "score", precision = 5, scale = 2)
    var score: BigDecimal? = null,

    @Column(name = "comment", columnDefinition = "text")
    var comment: String? = null,

    @Column(name = "result_summary", columnDefinition = "text")
    var resultSummary: String? = null,

    @Column(name = "log_object_key", length = 500)
    var logObjectKey: String? = null,

    @Column(name = "exit_code")
    var exitCode: Int? = null,

    @Column(name = "tests_passed")
    var testsPassed: Int? = null,

    @Column(name = "tests_total")
    var testsTotal: Int? = null,

    @Column(name = "scoring_mode", length = 30)
    var scoringMode: String? = null,

    @Column(name = "queued_at", nullable = false)
    var queuedAt: Instant = Instant.now(),

    @Column(name = "started_at")
    var startedAt: Instant? = null,

    @Column(name = "finished_at")
    var finishedAt: Instant? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
