package ru.coderoom.course.security

import java.util.UUID

data class RequestUser(
    val userId: UUID,
    val role: GlobalRole,
)
