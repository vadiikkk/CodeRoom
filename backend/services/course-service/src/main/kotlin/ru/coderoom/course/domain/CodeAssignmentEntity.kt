package ru.coderoom.course.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "code_assignments")
class CodeAssignmentEntity(
    @Id
    @Column(name = "assignment_id", nullable = false)
    val assignmentId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(name = "language", nullable = false, length = 20)
    var language: CodeLanguage,

    @Column(name = "repository_name", nullable = false, length = 120)
    var repositoryName: String,

    @Column(name = "repository_full_name", nullable = false, length = 255)
    var repositoryFullName: String,

    @Column(name = "repository_url", nullable = false, length = 500)
    var repositoryUrl: String,

    @Column(name = "default_branch", nullable = false, length = 120)
    var defaultBranch: String,

    @Column(name = "max_attempts", nullable = false)
    var maxAttempts: Int,

    @Column(name = "private_tests_attachment_id")
    var privateTestsAttachmentId: UUID? = null,

    @Column(name = "repository_private", nullable = false)
    var repositoryPrivate: Boolean = true,

    @Column(name = "starter_config", nullable = false, columnDefinition = "text")
    var starterConfig: String,

    @Column(name = "published_at")
    var publishedAt: Instant? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
