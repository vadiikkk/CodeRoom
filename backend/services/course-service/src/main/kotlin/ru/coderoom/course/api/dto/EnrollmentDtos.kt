package ru.coderoom.course.api.dto

import jakarta.validation.constraints.NotNull
import java.time.Instant
import java.util.UUID
import ru.coderoom.course.domain.RoleInCourse

data class UpsertEnrollmentRequest(
    @field:NotNull(message = "userId is required")
    val userId: UUID,

    @field:NotNull(message = "roleInCourse is required")
    val roleInCourse: RoleInCourse,
)

data class EnrollmentResponse(
    val userId: UUID,
    val roleInCourse: RoleInCourse,
    val createdAt: Instant,
)

data class MyMembershipResponse(
    val courseId: UUID,
    val userId: UUID,
    val roleInCourse: RoleInCourse,
)
