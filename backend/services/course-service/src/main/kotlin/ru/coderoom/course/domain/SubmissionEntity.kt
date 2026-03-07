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
@Table(name = "submissions")
class SubmissionEntity(
    @Id
    @Column(name = "submission_id", nullable = false)
    val submissionId: UUID,

    @Column(name = "course_id", nullable = false)
    val courseId: UUID,

    @Column(name = "assignment_id", nullable = false)
    val assignmentId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false, length = 20)
    val ownerType: SubmissionOwnerType,

    @Column(name = "owner_user_id")
    val ownerUserId: UUID? = null,

    @Column(name = "owner_group_id")
    val ownerGroupId: UUID? = null,

    @Column(name = "text_answer", columnDefinition = "text")
    var textAnswer: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: SubmissionStatus,

    @Column(name = "score", precision = 5, scale = 2)
    var score: BigDecimal? = null,

    @Column(name = "comment", columnDefinition = "text")
    var comment: String? = null,

    @Column(name = "graded_by_user_id")
    var gradedByUserId: UUID? = null,

    @Column(name = "graded_at")
    var gradedAt: Instant? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "grader_type", length = 20)
    var graderType: SubmissionGraderType? = null,

    @Column(name = "autograde_status", length = 30)
    var autogradeStatus: String? = null,

    @Column(name = "external_check_status", length = 30)
    var externalCheckStatus: String? = null,

    @Column(name = "submitted_at", nullable = false)
    val submittedAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
