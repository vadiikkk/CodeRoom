package ru.coderoom.course.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import java.io.Serializable
import java.time.Instant
import java.util.UUID

data class CourseGroupMemberId(
    val groupId: UUID = UUID(0, 0),
    val userId: UUID = UUID(0, 0),
) : Serializable

@Entity
@Table(name = "course_group_members")
@IdClass(CourseGroupMemberId::class)
class CourseGroupMemberEntity(
    @Id
    @Column(name = "group_id", nullable = false)
    val groupId: UUID,

    @Column(name = "course_id", nullable = false)
    val courseId: UUID,

    @Id
    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
)
