package ru.coderoom.identity.auth

import io.jsonwebtoken.Claims
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import ru.coderoom.identity.user.UserEntity
import ru.coderoom.identity.user.UserRepository
import ru.coderoom.identity.user.UserRole
import java.time.Instant
import java.util.Optional
import java.util.UUID

class JwtAuthFilterTest {
    private val jwtService = mockk<JwtService>()
    private val users = mockk<UserRepository>()
    private val filter = JwtAuthFilter(jwtService, users)

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun noAuthorizationHeader_doesNotAuthenticate() {
        val req = MockHttpServletRequest()
        val res = MockHttpServletResponse()
        val chain = MockFilterChain()

        filter.doFilter(req, res, chain)

        assertNull(SecurityContextHolder.getContext().authentication)
        verify(exactly = 0) { jwtService.parseClaims(any()) }
        verify(exactly = 0) { users.findById(any<UUID>()) }
    }

    @Test
    fun nonBearerAuthorizationHeader_doesNotAuthenticate() {
        val req = MockHttpServletRequest().apply {
            addHeader("Authorization", "Basic abc")
        }
        val res = MockHttpServletResponse()
        val chain = MockFilterChain()

        filter.doFilter(req, res, chain)

        assertNull(SecurityContextHolder.getContext().authentication)
        verify(exactly = 0) { jwtService.parseClaims(any()) }
        verify(exactly = 0) { users.findById(any<UUID>()) }
    }

    @Test
    fun invalidJwt_isIgnored_andDoesNotAuthenticate() {
        val req = MockHttpServletRequest().apply {
            addHeader("Authorization", "Bearer bad-token")
        }
        val res = MockHttpServletResponse()
        val chain = MockFilterChain()

        every { jwtService.parseClaims("bad-token") } throws IllegalArgumentException("bad")

        filter.doFilter(req, res, chain)

        assertNull(SecurityContextHolder.getContext().authentication)
        verify(exactly = 1) { jwtService.parseClaims("bad-token") }
        verify(exactly = 0) { users.findById(any<UUID>()) }
    }

    @Test
    fun userNotFound_doesNotAuthenticate() {
        val userId = UUID.randomUUID()
        val claims = mockk<Claims>()
        every { claims.subject } returns userId.toString()
        every { jwtService.parseClaims("token") } returns claims
        every { users.findById(userId) } returns Optional.empty()

        val req = MockHttpServletRequest().apply {
            addHeader("Authorization", "Bearer token")
        }
        val res = MockHttpServletResponse()
        val chain = MockFilterChain()

        filter.doFilter(req, res, chain)

        assertNull(SecurityContextHolder.getContext().authentication)
        verify(exactly = 1) { jwtService.parseClaims("token") }
        verify(exactly = 1) { users.findById(userId) }
    }

    @Test
    fun inactiveUser_doesNotAuthenticate() {
        val userId = UUID.randomUUID()
        val claims = mockk<Claims>()
        every { claims.subject } returns userId.toString()
        every { jwtService.parseClaims("token") } returns claims

        val user = userEntity(userId = userId, isActive = false, isRoot = true, role = UserRole.TEACHER)
        every { users.findById(userId) } returns Optional.of(user)

        val req = MockHttpServletRequest().apply {
            addHeader("Authorization", "Bearer token")
        }
        val res = MockHttpServletResponse()
        val chain = MockFilterChain()

        filter.doFilter(req, res, chain)

        assertNull(SecurityContextHolder.getContext().authentication)
        verify(exactly = 1) { jwtService.parseClaims("token") }
        verify(exactly = 1) { users.findById(userId) }
    }

    @Test
    fun activeUser_setsAuthentication_withAuthorities() {
        val userId = UUID.randomUUID()
        val claims = mockk<Claims>()
        every { claims.subject } returns userId.toString()
        every { jwtService.parseClaims("token") } returns claims

        val user = userEntity(userId = userId, isActive = true, isRoot = true, role = UserRole.TEACHER)
        every { users.findById(userId) } returns Optional.of(user)

        val req = MockHttpServletRequest().apply {
            addHeader("Authorization", "Bearer token")
        }
        val res = MockHttpServletResponse()
        val chain = MockFilterChain()

        filter.doFilter(req, res, chain)

        val auth = SecurityContextHolder.getContext().authentication
        assertTrue(auth != null && auth.isAuthenticated)
        val principal = auth!!.principal as UserPrincipal
        assertEquals(userId, principal.userId)
        assertEquals("user@example.com", principal.email)
        assertEquals(UserRole.TEACHER, principal.role)

        val authorities = auth.authorities.map { it.authority }.toSet()
        assertTrue(authorities.contains("ROLE_TEACHER"))
        assertTrue(authorities.contains("ROLE_ROOT"))

        verify(exactly = 1) { jwtService.parseClaims("token") }
        verify(exactly = 1) { users.findById(userId) }
    }

    private fun userEntity(
        userId: UUID,
        email: String = "user@example.com",
        role: UserRole,
        isRoot: Boolean,
        isActive: Boolean,
    ): UserEntity =
        UserEntity(
            userId = userId,
            email = email,
            passwordHash = "hash",
            role = role,
            isRoot = isRoot,
            isActive = isActive,
            createdAt = Instant.now(),
        )
}
