package ru.coderoom.identity.user

import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

data class LookupUsersByEmailRequest(
    @field:NotEmpty
    val emails: List<String>,
)

data class LookupUsersByIdRequest(
    @field:NotEmpty
    val userIds: List<UUID>,
)

data class LookupUserDto(
    val userId: UUID,
    val email: String,
)

data class LookupUsersByEmailResponse(
    val users: List<LookupUserDto>,
)

@RestController
@RequestMapping("/api/v1/users")
class UsersLookupController(
    private val users: UserRepository,
) {
    @PostMapping("/lookup")
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('TEACHER')")
    fun lookupByEmail(
        @RequestBody @Valid req: LookupUsersByEmailRequest,
    ): LookupUsersByEmailResponse {
        val normalized = req.emails.map { it.trim().lowercase() }.filter { it.isNotBlank() }.distinct()
        if (normalized.isEmpty()) return LookupUsersByEmailResponse(users = emptyList())

        val found = normalized.mapNotNull { email -> users.findByEmail(email) }
        return LookupUsersByEmailResponse(
            users = found.map { LookupUserDto(userId = it.userId, email = it.email) },
        )
    }

    @PostMapping("/lookup-by-ids")
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('TEACHER','ASSISTANT')")
    fun lookupByIds(
        @RequestBody @Valid req: LookupUsersByIdRequest,
    ): LookupUsersByEmailResponse {
        val safe = req.userIds.distinct()
        if (safe.isEmpty()) return LookupUsersByEmailResponse(users = emptyList())

        val foundById = users.findAllByUserIdIn(safe).associateBy { it.userId }
        return LookupUsersByEmailResponse(
            users = safe.mapNotNull { userId ->
                foundById[userId]?.let { LookupUserDto(userId = it.userId, email = it.email) }
            },
        )
    }
}
