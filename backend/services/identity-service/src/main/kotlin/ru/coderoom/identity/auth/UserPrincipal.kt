package ru.coderoom.identity.auth

import ru.coderoom.identity.user.UserRole
import java.util.UUID

data class UserPrincipal(
    val userId: UUID,
    val email: String,
    val role: UserRole,
)
