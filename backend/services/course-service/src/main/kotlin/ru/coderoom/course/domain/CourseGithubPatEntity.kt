package ru.coderoom.course.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "course_github_pat")
class CourseGithubPatEntity(
    @Id
    @Column(name = "course_id", nullable = false)
    val courseId: UUID,

    @Column(name = "iv", nullable = false)
    var iv: ByteArray,

    @Column(name = "ciphertext", nullable = false)
    var ciphertext: ByteArray,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
