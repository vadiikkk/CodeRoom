package ru.coderoom.course.api.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class CreateGroupRequest(
    @field:NotBlank(message = "name is required")
    @field:Size(max = 200, message = "name too long")
    val name: String,
)

data class UpdateGroupRequest(
    @field:NotBlank(message = "name is required")
    @field:Size(max = 200, message = "name too long")
    val name: String,
)

data class AddGroupMemberRequest(
    @field:NotNull(message = "userId is required")
    val userId: UUID,
)

data class GroupMemberResponse(
    val userId: UUID,
    val createdAt: Instant,
)

data class GroupResponse(
    val groupId: UUID,
    val courseId: UUID,
    val name: String,
    val members: List<GroupMemberResponse>,
    val createdAt: Instant,
    val updatedAt: Instant,
)
