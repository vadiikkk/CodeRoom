package ru.coderoom.course.api.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

data class SetGithubPatRequest(
    @field:NotBlank(message = "token is required")
    @field:Size(min = 10, max = 10_000, message = "token length is invalid")
    val token: String,
)

data class GithubPatStatusResponse(
    val configured: Boolean,
    val updatedAt: Instant?,
)
