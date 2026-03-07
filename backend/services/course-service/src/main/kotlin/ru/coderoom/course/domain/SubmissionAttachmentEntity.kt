package ru.coderoom.course.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import java.io.Serializable
import java.time.Instant
import java.util.UUID

data class SubmissionAttachmentId(
    val submissionId: UUID = UUID(0, 0),
    val attachmentId: UUID = UUID(0, 0),
) : Serializable

@Entity
@Table(name = "submission_attachments")
@IdClass(SubmissionAttachmentId::class)
class SubmissionAttachmentEntity(
    @Id
    @Column(name = "submission_id", nullable = false)
    val submissionId: UUID,

    @Id
    @Column(name = "attachment_id", nullable = false)
    val attachmentId: UUID,

    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int = 0,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
)
