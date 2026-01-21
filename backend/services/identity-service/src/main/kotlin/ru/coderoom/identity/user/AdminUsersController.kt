package ru.coderoom.identity.user

import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.server.ResponseStatusException
import ru.coderoom.identity.auth.RefreshTokenRepository
import ru.coderoom.identity.auth.UserPrincipal
import java.time.Instant
import java.util.UUID

data class SetUserRoleRequest(
    @field:Email
    @field:NotBlank
    val email: String,

    val role: UserRole,
)

data class SetUserActiveRequest(
    @field:Email
    @field:NotBlank
    val email: String,

    val isActive: Boolean,
)

data class AdminUserResponse(
    val userId: UUID,
    val email: String,
    val role: UserRole,
    val isRoot: Boolean,
    val isActive: Boolean,
    val createdAt: Instant,
)

@RestController
@RequestMapping("/api/v1/admin/users")
class AdminUsersController(
    private val users: UserRepository,
    private val refreshTokens: RefreshTokenRepository,
) {
    @GetMapping
    fun listUsers(
        auth: Authentication,
        @RequestParam(required = false, defaultValue = "") q: String,
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "50") size: Int,
    ): List<AdminUserResponse> {
        requireRoot(auth)

        val safeSize = size.coerceIn(1, 200)
        val pageable = PageRequest.of(page.coerceAtLeast(0), safeSize)

        val pageResult =
            if (q.isBlank()) users.findAll(pageable) else users.findByEmailContainingIgnoreCase(q.trim(), pageable)

        return pageResult.content.map {
            AdminUserResponse(
                userId = it.userId,
                email = it.email,
                role = it.role,
                isRoot = it.isRoot,
                isActive = it.isActive,
                createdAt = it.createdAt,
            )
        }
    }

    @PutMapping("/role")
    @Transactional
    fun setRole(
        auth: Authentication,
        @RequestBody @Valid req: SetUserRoleRequest,
    ) {
        requireRoot(auth)

        val targetEmail = req.email.trim().lowercase()
        val target = users.findByEmail(targetEmail)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")

        if (target.isRoot) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot change root user role")
        }

        val updated = users.updateRoleByUserId(target.userId, req.role)
        if (updated == 0) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        }
    }

    @PutMapping("/active")
    @Transactional
    fun setActive(
        auth: Authentication,
        @RequestBody @Valid req: SetUserActiveRequest,
    ) {
        requireRoot(auth)

        val targetEmail = req.email.trim().lowercase()
        val target = users.findByEmail(targetEmail)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")

        if (target.isRoot && !req.isActive) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot deactivate root user")
        }

        val updated = users.updateActiveByUserId(target.userId, req.isActive)
        if (updated == 0) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        }

        if (!req.isActive) {
            refreshTokens.revokeAllByUserId(target.userId, Instant.now())
        }
    }

    private fun requireRoot(auth: Authentication) {
        val principal = auth.principal as? UserPrincipal
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized")

        val actor = users.findById(principal.userId).orElseThrow {
            ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized")
        }
        if (!actor.isRoot) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden")
        }
    }
}
