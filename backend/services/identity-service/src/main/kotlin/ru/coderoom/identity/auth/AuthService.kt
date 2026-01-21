package ru.coderoom.identity.auth

import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.coderoom.identity.user.UserEntity
import ru.coderoom.identity.user.UserRepository
import ru.coderoom.identity.user.UserRole
import java.time.Instant
import java.util.UUID

data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
)

@Service
class AuthService(
    private val users: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val refreshTokens: RefreshTokenRepository,
    private val jwtProps: JwtProperties,
) {
    fun register(email: String, password: String): AuthTokens {
        if (users.existsByEmail(email)) {
            throw IllegalArgumentException("Email already registered")
        }

        val user = UserEntity(
            userId = UUID.randomUUID(),
            email = email.lowercase(),
            passwordHash = passwordEncoder.encode(password),
            role = UserRole.STUDENT,
            isRoot = false,
            isActive = true,
            createdAt = Instant.now(),
        )
        users.save(user)
        return issueTokens(user)
    }

    fun login(email: String, password: String): AuthTokens {
        val user = users.findByEmail(email.lowercase())
            ?: throw IllegalArgumentException("Invalid credentials")

        if (!user.isActive) {
            throw IllegalArgumentException("Invalid credentials")
        }

        if (!passwordEncoder.matches(password, user.passwordHash)) {
            throw IllegalArgumentException("Invalid credentials")
        }

        return issueTokens(user)
    }

    fun refresh(refreshToken: String): AuthTokens {
        val now = Instant.now()
        val tokenHash = TokenUtils.sha256Hex(refreshToken)
        val stored = refreshTokens.findByTokenHash(tokenHash)
            ?: throw IllegalArgumentException("Invalid refresh token")

        if (stored.revokedAt != null || stored.expiresAt.isBefore(now)) {
            throw IllegalArgumentException("Invalid refresh token")
        }

        stored.revokedAt = now
        refreshTokens.save(stored)

        val user = users.findById(stored.userId).orElseThrow { IllegalArgumentException("User not found") }
        if (!user.isActive) {
            throw IllegalArgumentException("Invalid refresh token")
        }
        return issueTokens(user)
    }

    @Transactional
    fun logout(refreshToken: String) {
        val now = Instant.now()
        val tokenHash = TokenUtils.sha256Hex(refreshToken)
        val stored = refreshTokens.findByTokenHash(tokenHash) ?: return
        if (stored.revokedAt == null) {
            stored.revokedAt = now
            refreshTokens.save(stored)
        }
    }

    @Transactional
    fun logoutAll(refreshToken: String) {
        val now = Instant.now()
        val tokenHash = TokenUtils.sha256Hex(refreshToken)
        val stored = refreshTokens.findByTokenHash(tokenHash) ?: return

        refreshTokens.revokeAllByUserId(stored.userId, now)
    }

    @Transactional
    fun changePassword(userId: UUID, oldPassword: String, newPassword: String) {
        val user = users.findById(userId).orElseThrow { IllegalArgumentException("User not found") }
        if (!passwordEncoder.matches(oldPassword, user.passwordHash)) {
            throw IllegalArgumentException("Invalid credentials")
        }
        user.passwordHash = passwordEncoder.encode(newPassword)
        users.save(user)
        refreshTokens.revokeAllByUserId(userId, Instant.now())
    }

    private fun issueTokens(user: UserEntity): AuthTokens {
        val now = Instant.now()
        val accessToken = jwtService.issueAccessToken(user.userId, user.email, user.role)

        val refreshToken = TokenUtils.newRefreshTokenValue()
        val refreshTokenHash = TokenUtils.sha256Hex(refreshToken)

        refreshTokens.save(
            RefreshTokenEntity(
                id = UUID.randomUUID(),
                userId = user.userId,
                tokenHash = refreshTokenHash,
                createdAt = now,
                expiresAt = now.plusSeconds(jwtProps.refreshTokenTtlSeconds),
                revokedAt = null,
            ),
        )

        return AuthTokens(
            accessToken = accessToken,
            refreshToken = refreshToken,
        )
    }
}
