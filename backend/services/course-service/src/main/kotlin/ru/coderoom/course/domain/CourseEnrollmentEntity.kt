package ru.coderoom.course.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import java.io.Serializable
import java.time.Instant
import java.util.UUID

data class CourseEnrollmentId(
    val courseId: UUID = UUID(0, 0),
    val userId: UUID = UUID(0, 0),
) : Serializable

@Entity
@Table(name = "course_enrollments")
@IdClass(CourseEnrollmentId::class)
class CourseEnrollmentEntity(
    @Id
    @Column(name = "course_id", nullable = false)
    val courseId: UUID,

    @Id
    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(name = "role_in_course", nullable = false, length = 20)
    var roleInCourse: RoleInCourse,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
)
