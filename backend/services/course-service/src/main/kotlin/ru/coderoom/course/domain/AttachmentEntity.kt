package ru.coderoom.course.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "course_attachments")
class AttachmentEntity(
    @Id
    @Column(name = "attachment_id", nullable = false)
    val attachmentId: UUID,

    @Column(name = "course_id", nullable = false)
    val courseId: UUID,

    @Column(name = "material_id")
    var materialId: UUID? = null,

    @Column(name = "assignment_id")
    var assignmentId: UUID? = null,

    @Column(name = "storage_bucket", nullable = false, length = 120)
    val storageBucket: String,

    @Column(name = "object_key", nullable = false, length = 500)
    val objectKey: String,

    @Column(name = "original_filename", nullable = false, length = 255)
    val originalFilename: String,

    @Column(name = "content_type", length = 255)
    val contentType: String? = null,

    @Column(name = "file_size", nullable = false)
    val fileSize: Long,

    @Column(name = "uploaded_by_user_id", nullable = false)
    val uploadedByUserId: UUID,

    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int = 0,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
