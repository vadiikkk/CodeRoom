package ru.coderoom.course.api.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class CreateCourseRequest(
    @field:NotBlank(message = "title is required")
    @field:Size(max = 200, message = "title too long")
    val title: String,

    @field:Size(max = 10_000, message = "description too long")
    val description: String? = null,

    val isVisible: Boolean = false,
)

data class UpdateCourseRequest(
    @field:Size(max = 200, message = "title too long")
    val title: String? = null,

    @field:Size(max = 10_000, message = "description too long")
    val description: String? = null,

    val isVisible: Boolean? = null,
)

data class CourseResponse(
    val courseId: UUID,
    val ownerUserId: UUID,
    val title: String,
    val description: String?,
    val isVisible: Boolean,
    val githubPatConfigured: Boolean,
    val myRoleInCourse: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)
