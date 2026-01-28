package ru.coderoom.course.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "courses")
class CourseEntity(
    @Id
    @Column(name = "course_id", nullable = false)
    val courseId: UUID,

    @Column(name = "owner_user_id", nullable = false)
    val ownerUserId: UUID,

    @Column(name = "title", nullable = false, length = 200)
    var title: String,

    @Column(name = "description", columnDefinition = "text")
    var description: String? = null,

    @Column(name = "is_visible", nullable = false)
    var isVisible: Boolean = false,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
