package ru.coderoom.identity.auth

import io.mockk.CapturingSlot
import io.mockk.clearMocks
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.crypto.password.PasswordEncoder
import ru.coderoom.identity.user.UserEntity
import ru.coderoom.identity.user.UserRepository
import ru.coderoom.identity.user.UserRole
import java.time.Instant
import java.util.Optional
import java.util.UUID

class AuthServiceTest {
    private lateinit var users: UserRepository
    private lateinit var passwordEncoder: PasswordEncoder
    private lateinit var jwtService: JwtService
    private lateinit var refreshTokens: RefreshTokenRepository
    private lateinit var jwtProps: JwtProperties
    private lateinit var service: AuthService

    @BeforeEach
    fun setUp() {
        users = mockk()
        passwordEncoder = mockk()
        jwtService = mockk()
        refreshTokens = mockk()
        jwtProps = JwtProperties(
            secret = "01234567890123456789012345678901",
            accessTokenTtlSeconds = 3600,
            refreshTokenTtlSeconds = 3600,
        )
        service = AuthService(users, passwordEncoder, jwtService, refreshTokens, jwtProps)
    }

    @Test
    fun register_whenEmailAlreadyRegistered_throws() {
        every { users.existsByEmail("user@example.com") } returns true

        val ex = assertThrows<IllegalArgumentException> {
            service.register("user@example.com", "password123")
        }
        assertEquals("Email already registered", ex.message)

        verify(exactly = 1) { users.existsByEmail("user@example.com") }
        confirmVerified(users)
    }

    @Test
    fun register_savesLowercasedEmail_andIssuesTokens() {
        val savedUsers = mutableListOf<UserEntity>()
        val savedRefreshTokens = mutableListOf<RefreshTokenEntity>()

        every { users.existsByEmail("USER@EXAMPLE.COM") } returns false
        every { passwordEncoder.encode("password123") } returns "hash"
        every { users.save(capture(savedUsers)) } answers { firstArg() }
        every { jwtService.issueAccessToken(any(), any(), any()) } returns "access"
        every { refreshTokens.save(capture(savedRefreshTokens)) } answers { firstArg() }

        val tokens = service.register("USER@EXAMPLE.COM", "password123")

        assertEquals("access", tokens.accessToken)
        assertTrue(tokens.refreshToken.isNotBlank())

        assertEquals(1, savedUsers.size)
        assertEquals("user@example.com", savedUsers.single().email)
        assertEquals(UserRole.STUDENT, savedUsers.single().role)
        assertTrue(savedUsers.single().isActive)

        assertEquals(1, savedRefreshTokens.size)
        assertEquals(savedUsers.single().userId, savedRefreshTokens.single().userId)
        assertNotNull(savedRefreshTokens.single().expiresAt)

        verify(exactly = 1) { users.existsByEmail("USER@EXAMPLE.COM") }
        verify(exactly = 1) { passwordEncoder.encode("password123") }
        verify(exactly = 1) { users.save(any()) }
        verify(exactly = 1) { jwtService.issueAccessToken(savedUsers.single().userId, "user@example.com", UserRole.STUDENT) }
        verify(exactly = 1) { refreshTokens.save(any()) }
        confirmVerified(users, passwordEncoder, jwtService, refreshTokens)
    }

    @Test
    fun login_whenUserNotFound_throws() {
        every { users.findByEmail("user@example.com") } returns null

        val ex = assertThrows<IllegalArgumentException> {
            service.login("USER@EXAMPLE.COM", "password123")
        }
        assertEquals("Invalid credentials", ex.message)

        verify(exactly = 1) { users.findByEmail("user@example.com") }
        confirmVerified(users)
    }

    @Test
    fun login_whenUserInactive_throws() {
        val user = userEntity(email = "user@example.com", isActive = false)
        every { users.findByEmail("user@example.com") } returns user

        val ex = assertThrows<IllegalArgumentException> {
            service.login("user@example.com", "password123")
        }
        assertEquals("Invalid credentials", ex.message)

        verify(exactly = 1) { users.findByEmail("user@example.com") }
        confirmVerified(users)
    }

    @Test
    fun login_whenPasswordMismatch_throws() {
        val user = userEntity(email = "user@example.com", passwordHash = "hash", isActive = true)
        every { users.findByEmail("user@example.com") } returns user
        every { passwordEncoder.matches("password123", "hash") } returns false

        val ex = assertThrows<IllegalArgumentException> {
            service.login("user@example.com", "password123")
        }
        assertEquals("Invalid credentials", ex.message)

        verify(exactly = 1) { users.findByEmail("user@example.com") }
        verify(exactly = 1) { passwordEncoder.matches("password123", "hash") }
        confirmVerified(users, passwordEncoder)
    }

    @Test
    fun login_success_issuesTokens() {
        val savedRefreshTokens = mutableListOf<RefreshTokenEntity>()
        val user = userEntity(email = "user@example.com", passwordHash = "hash", isActive = true)

        every { users.findByEmail("user@example.com") } returns user
        every { passwordEncoder.matches("password123", "hash") } returns true
        every { jwtService.issueAccessToken(user.userId, "user@example.com", user.role) } returns "access"
        every { refreshTokens.save(capture(savedRefreshTokens)) } answers { firstArg() }

        val tokens = service.login("USER@EXAMPLE.COM", "password123")

        assertEquals("access", tokens.accessToken)
        assertTrue(tokens.refreshToken.isNotBlank())
        assertEquals(1, savedRefreshTokens.size)
        assertEquals(user.userId, savedRefreshTokens.single().userId)

        verify(exactly = 1) { users.findByEmail("user@example.com") }
        verify(exactly = 1) { passwordEncoder.matches("password123", "hash") }
        verify(exactly = 1) { jwtService.issueAccessToken(user.userId, "user@example.com", user.role) }
        verify(exactly = 1) { refreshTokens.save(any()) }
        confirmVerified(users, passwordEncoder, jwtService, refreshTokens)
    }

    @Test
    fun refresh_whenTokenNotFound_throws() {
        val refreshToken = "refresh"
        val tokenHash = TokenUtils.sha256Hex(refreshToken)

        every { refreshTokens.findByTokenHash(tokenHash) } returns null

        val ex = assertThrows<IllegalArgumentException> {
            service.refresh(refreshToken)
        }
        assertEquals("Invalid refresh token", ex.message)

        verify(exactly = 1) { refreshTokens.findByTokenHash(tokenHash) }
        confirmVerified(refreshTokens)
    }

    @Test
    fun refresh_whenTokenRevoked_throws() {
        val refreshToken = "refresh"
        val tokenHash = TokenUtils.sha256Hex(refreshToken)
        val stored = refreshTokenEntity(tokenHash = tokenHash, revokedAt = Instant.now(), expiresAt = Instant.now().plusSeconds(3600))
        every { refreshTokens.findByTokenHash(tokenHash) } returns stored

        val ex = assertThrows<IllegalArgumentException> {
            service.refresh(refreshToken)
        }
        assertEquals("Invalid refresh token", ex.message)

        verify(exactly = 1) { refreshTokens.findByTokenHash(tokenHash) }
        confirmVerified(refreshTokens)
    }

    @Test
    fun refresh_whenTokenExpired_throws() {
        val refreshToken = "refresh"
        val tokenHash = TokenUtils.sha256Hex(refreshToken)
        val stored = refreshTokenEntity(tokenHash = tokenHash, revokedAt = null, expiresAt = Instant.EPOCH)
        every { refreshTokens.findByTokenHash(tokenHash) } returns stored

        val ex = assertThrows<IllegalArgumentException> {
            service.refresh(refreshToken)
        }
        assertEquals("Invalid refresh token", ex.message)

        verify(exactly = 1) { refreshTokens.findByTokenHash(tokenHash) }
        confirmVerified(refreshTokens)
    }

    @Test
    fun refresh_whenUserNotFound_revokesStoredToken_thenThrows() {
        val savedRefreshTokens = mutableListOf<RefreshTokenEntity>()
        val refreshToken = "refresh"
        val tokenHash = TokenUtils.sha256Hex(refreshToken)
        val stored = refreshTokenEntity(tokenHash = tokenHash, revokedAt = null, expiresAt = Instant.now().plusSeconds(3600))

        every { refreshTokens.findByTokenHash(tokenHash) } returns stored
        every { refreshTokens.save(capture(savedRefreshTokens)) } answers { firstArg() }
        every { users.findById(stored.userId) } returns Optional.empty()

        val ex = assertThrows<IllegalArgumentException> {
            service.refresh(refreshToken)
        }
        assertEquals("User not found", ex.message)
        assertNotNull(stored.revokedAt)

        assertEquals(1, savedRefreshTokens.size)
        assertEquals(tokenHash, savedRefreshTokens.single().tokenHash)

        verify(exactly = 1) { refreshTokens.findByTokenHash(tokenHash) }
        verify(exactly = 1) { refreshTokens.save(stored) }
        verify(exactly = 1) { users.findById(stored.userId) }
        confirmVerified(refreshTokens, users)
    }

    @Test
    fun refresh_whenUserInactive_revokesStoredToken_thenThrows() {
        val savedRefreshTokens = mutableListOf<RefreshTokenEntity>()
        val refreshToken = "refresh"
        val tokenHash = TokenUtils.sha256Hex(refreshToken)
        val stored = refreshTokenEntity(tokenHash = tokenHash, revokedAt = null, expiresAt = Instant.now().plusSeconds(3600))
        val user = userEntity(userId = stored.userId, email = "user@example.com", isActive = false)

        every { refreshTokens.findByTokenHash(tokenHash) } returns stored
        every { refreshTokens.save(capture(savedRefreshTokens)) } answers { firstArg() }
        every { users.findById(stored.userId) } returns Optional.of(user)

        val ex = assertThrows<IllegalArgumentException> {
            service.refresh(refreshToken)
        }
        assertEquals("Invalid refresh token", ex.message)
        assertNotNull(stored.revokedAt)

        assertEquals(1, savedRefreshTokens.size)
        assertEquals(tokenHash, savedRefreshTokens.single().tokenHash)

        verify(exactly = 1) { refreshTokens.findByTokenHash(tokenHash) }
        verify(exactly = 1) { refreshTokens.save(stored) }
        verify(exactly = 1) { users.findById(stored.userId) }
        confirmVerified(refreshTokens, users)
    }

    @Test
    fun refresh_success_revokesStoredToken_andIssuesNewTokens() {
        val savedRefreshTokens = mutableListOf<RefreshTokenEntity>()
        val refreshToken = "refresh"
        val tokenHash = TokenUtils.sha256Hex(refreshToken)
        val stored = refreshTokenEntity(tokenHash = tokenHash, revokedAt = null, expiresAt = Instant.now().plusSeconds(3600))
        val user = userEntity(userId = stored.userId, email = "user@example.com", isActive = true)

        every { refreshTokens.findByTokenHash(tokenHash) } returns stored
        every { refreshTokens.save(capture(savedRefreshTokens)) } answers { firstArg() }
        every { users.findById(stored.userId) } returns Optional.of(user)
        every { jwtService.issueAccessToken(user.userId, user.email, user.role) } returns "access2"

        val tokens = service.refresh(refreshToken)

        assertEquals("access2", tokens.accessToken)
        assertTrue(tokens.refreshToken.isNotBlank())
        assertNotNull(stored.revokedAt)
        assertEquals(2, savedRefreshTokens.size)

        verify(exactly = 1) { refreshTokens.findByTokenHash(tokenHash) }
        verify(exactly = 2) { refreshTokens.save(any()) }
        verify(exactly = 1) { users.findById(stored.userId) }
        verify(exactly = 1) { jwtService.issueAccessToken(user.userId, user.email, user.role) }
        confirmVerified(refreshTokens, users, jwtService)
    }

    @Test
    fun logout_whenTokenNotFound_doesNothing() {
        val refreshToken = "refresh"
        val tokenHash = TokenUtils.sha256Hex(refreshToken)
        every { refreshTokens.findByTokenHash(tokenHash) } returns null

        service.logout(refreshToken)

        verify(exactly = 1) { refreshTokens.findByTokenHash(tokenHash) }
        verify(exactly = 0) { refreshTokens.save(any()) }
        confirmVerified(refreshTokens)
    }

    @Test
    fun logout_whenTokenAlreadyRevoked_doesNothing() {
        val refreshToken = "refresh"
        val tokenHash = TokenUtils.sha256Hex(refreshToken)
        val stored = refreshTokenEntity(tokenHash = tokenHash, revokedAt = Instant.now(), expiresAt = Instant.now().plusSeconds(3600))
        every { refreshTokens.findByTokenHash(tokenHash) } returns stored

        service.logout(refreshToken)

        verify(exactly = 1) { refreshTokens.findByTokenHash(tokenHash) }
        verify(exactly = 0) { refreshTokens.save(any()) }
        confirmVerified(refreshTokens)
    }

    @Test
    fun logout_whenTokenActive_revokesAndSaves() {
        val refreshToken = "refresh"
        val tokenHash = TokenUtils.sha256Hex(refreshToken)
        val stored = refreshTokenEntity(tokenHash = tokenHash, revokedAt = null, expiresAt = Instant.now().plusSeconds(3600))

        every { refreshTokens.findByTokenHash(tokenHash) } returns stored
        every { refreshTokens.save(stored) } returns stored

        service.logout(refreshToken)

        assertNotNull(stored.revokedAt)
        verify(exactly = 1) { refreshTokens.findByTokenHash(tokenHash) }
        verify(exactly = 1) { refreshTokens.save(stored) }
        confirmVerified(refreshTokens)
    }

    @Test
    fun logoutAll_whenTokenNotFound_doesNothing() {
        val refreshToken = "refresh"
        val tokenHash = TokenUtils.sha256Hex(refreshToken)
        every { refreshTokens.findByTokenHash(tokenHash) } returns null

        service.logoutAll(refreshToken)

        verify(exactly = 1) { refreshTokens.findByTokenHash(tokenHash) }
        verify(exactly = 0) { refreshTokens.revokeAllByUserId(any(), any()) }
        confirmVerified(refreshTokens)
    }

    @Test
    fun logoutAll_whenTokenFound_revokesAllByUserId() {
        val refreshToken = "refresh"
        val tokenHash = TokenUtils.sha256Hex(refreshToken)
        val stored = refreshTokenEntity(tokenHash = tokenHash, revokedAt = null, expiresAt = Instant.now().plusSeconds(3600))
        every { refreshTokens.findByTokenHash(tokenHash) } returns stored
        every { refreshTokens.revokeAllByUserId(stored.userId, any()) } returns 1

        service.logoutAll(refreshToken)

        verify(exactly = 1) { refreshTokens.findByTokenHash(tokenHash) }
        verify(exactly = 1) { refreshTokens.revokeAllByUserId(stored.userId, any()) }
        confirmVerified(refreshTokens)
    }

    @Test
    fun changePassword_whenUserNotFound_throws() {
        val userId = UUID.randomUUID()
        every { users.findById(userId) } returns Optional.empty()

        val ex = assertThrows<IllegalArgumentException> {
            service.changePassword(userId, "old", "new")
        }
        assertEquals("User not found", ex.message)

        verify(exactly = 1) { users.findById(userId) }
        confirmVerified(users)
    }

    @Test
    fun changePassword_whenOldPasswordMismatch_throws() {
        val user = userEntity(passwordHash = "hash")
        every { users.findById(user.userId) } returns Optional.of(user)
        every { passwordEncoder.matches("old", "hash") } returns false

        val ex = assertThrows<IllegalArgumentException> {
            service.changePassword(user.userId, "old", "new")
        }
        assertEquals("Invalid credentials", ex.message)

        verify(exactly = 1) { users.findById(user.userId) }
        verify(exactly = 1) { passwordEncoder.matches("old", "hash") }
        verify(exactly = 0) { users.save(any()) }
        verify(exactly = 0) { refreshTokens.revokeAllByUserId(any(), any()) }
        confirmVerified(users, passwordEncoder, refreshTokens)
    }

    @Test
    fun changePassword_success_updatesHash_andRevokesAllRefreshTokens() {
        val user = userEntity(passwordHash = "hash")
        every { users.findById(user.userId) } returns Optional.of(user)
        every { passwordEncoder.matches("old", "hash") } returns true
        every { passwordEncoder.encode("new") } returns "newHash"
        every { users.save(user) } returns user
        every { refreshTokens.revokeAllByUserId(user.userId, any()) } returns 1

        service.changePassword(user.userId, "old", "new")

        assertEquals("newHash", user.passwordHash)
        verify(exactly = 1) { users.findById(user.userId) }
        verify(exactly = 1) { passwordEncoder.matches("old", "hash") }
        verify(exactly = 1) { passwordEncoder.encode("new") }
        verify(exactly = 1) { users.save(user) }
        verify(exactly = 1) { refreshTokens.revokeAllByUserId(user.userId, any()) }
        confirmVerified(users, passwordEncoder, refreshTokens)
    }

    private fun userEntity(
        userId: UUID = UUID.randomUUID(),
        email: String = "user@example.com",
        passwordHash: String = "hash",
        role: UserRole = UserRole.STUDENT,
        isRoot: Boolean = false,
        isActive: Boolean = true,
    ): UserEntity =
        UserEntity(
            userId = userId,
            email = email,
            passwordHash = passwordHash,
            role = role,
            isRoot = isRoot,
            isActive = isActive,
            createdAt = Instant.now(),
        )

    private fun refreshTokenEntity(
        userId: UUID = UUID.randomUUID(),
        tokenHash: String,
        createdAt: Instant = Instant.now(),
        expiresAt: Instant,
        revokedAt: Instant?,
    ): RefreshTokenEntity =
        RefreshTokenEntity(
            id = UUID.randomUUID(),
            userId = userId,
            tokenHash = tokenHash,
            createdAt = createdAt,
            expiresAt = expiresAt,
            revokedAt = revokedAt,
        )
}
