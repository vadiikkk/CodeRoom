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

data class UpsertEnrollmentsByEmailRequest(
    @field:NotNull(message = "emails is required")
    val emails: List<String>,

    @field:NotNull(message = "roleInCourse is required")
    val roleInCourse: RoleInCourse,
)

data class UpsertEnrollmentsByEmailResponse(
    val addedOrUpdated: Int,
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
