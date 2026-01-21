package ru.coderoom.identity.user

import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.server.ResponseStatusException
import ru.coderoom.identity.auth.RefreshTokenRepository
import ru.coderoom.identity.auth.UserPrincipal
import java.time.Instant
import java.util.Optional
import java.util.UUID

class AdminUsersControllerTest {
    private val users = mockk<UserRepository>()
    private val refreshTokens = mockk<RefreshTokenRepository>()
    private val controller = AdminUsersController(users, refreshTokens)

    @Test
    fun listUsers_whenPrincipalIsNotUserPrincipal_throwsUnauthorized() {
        val auth = mockk<Authentication>()
        every { auth.principal } returns "not-a-principal"

        val ex = assertThrows<ResponseStatusException> {
            controller.listUsers(auth, q = "", page = 0, size = 50)
        }
        assertEquals(HttpStatus.UNAUTHORIZED, ex.statusCode)

        verify(exactly = 1) { auth.principal }
        confirmVerified(auth)
    }

    @Test
    fun listUsers_whenActorNotRoot_throwsForbidden() {
        val actorId = UUID.randomUUID()
        val principal = UserPrincipal(userId = actorId, email = "actor@example.com", role = UserRole.TEACHER)
        val auth = authWithPrincipal(principal)

        every { users.findById(actorId) } returns Optional.of(userEntity(userId = actorId, isRoot = false))

        val ex = assertThrows<ResponseStatusException> {
            controller.listUsers(auth, q = "", page = 0, size = 50)
        }
        assertEquals(HttpStatus.FORBIDDEN, ex.statusCode)

        verify(exactly = 1) { users.findById(actorId) }
        confirmVerified(users)
    }

    @Test
    fun listUsers_whenQueryBlank_usesFindAll_andAppliesPageAndSizeCoercion() {
        val actorId = UUID.randomUUID()
        val principal = UserPrincipal(userId = actorId, email = "actor@example.com", role = UserRole.TEACHER)
        val auth = authWithPrincipal(principal)

        every { users.findById(actorId) } returns Optional.of(userEntity(userId = actorId, isRoot = true))

        val pageableSlot = slot<Pageable>()
        every { users.findAll(capture(pageableSlot)) } returns PageImpl(listOf(userEntity(email = "u1@example.com")))

        val res = controller.listUsers(auth, q = "   ", page = -10, size = 999)

        assertEquals(0, pageableSlot.captured.pageNumber)
        assertEquals(200, pageableSlot.captured.pageSize)
        assertEquals(1, res.size)
        assertEquals("u1@example.com", res.single().email)

        verify(exactly = 1) { users.findById(actorId) }
        verify(exactly = 1) { users.findAll(any<Pageable>()) }
        confirmVerified(users)
    }

    @Test
    fun listUsers_whenQueryNotBlank_usesFindByEmailContainingIgnoreCase_withTrimmedQuery() {
        val actorId = UUID.randomUUID()
        val principal = UserPrincipal(userId = actorId, email = "actor@example.com", role = UserRole.TEACHER)
        val auth = authWithPrincipal(principal)

        every { users.findById(actorId) } returns Optional.of(userEntity(userId = actorId, isRoot = true))

        val pageableSlot = slot<Pageable>()
        every { users.findByEmailContainingIgnoreCase("TeSt", capture(pageableSlot)) } returns PageImpl(
            listOf(userEntity(email = "test@example.com")),
        )

        val res = controller.listUsers(auth, q = "  TeSt  ", page = 1, size = 0)

        assertEquals(1, pageableSlot.captured.pageNumber)
        assertEquals(1, pageableSlot.captured.pageSize)
        assertEquals("test@example.com", res.single().email)

        verify(exactly = 1) { users.findById(actorId) }
        verify(exactly = 1) { users.findByEmailContainingIgnoreCase("TeSt", any<Pageable>()) }
        confirmVerified(users)
    }

    @Test
    fun setRole_whenUserNotFound_throwsNotFound() {
        val actorId = UUID.randomUUID()
        val principal = UserPrincipal(userId = actorId, email = "actor@example.com", role = UserRole.TEACHER)
        val auth = authWithPrincipal(principal)

        every { users.findById(actorId) } returns Optional.of(userEntity(userId = actorId, isRoot = true))
        every { users.findByEmail("target@example.com") } returns null

        val ex = assertThrows<ResponseStatusException> {
            controller.setRole(auth, SetUserRoleRequest(email = " TARGET@EXAMPLE.COM ", role = UserRole.STUDENT))
        }
        assertEquals(HttpStatus.NOT_FOUND, ex.statusCode)

        verify(exactly = 1) { users.findById(actorId) }
        verify(exactly = 1) { users.findByEmail("target@example.com") }
        confirmVerified(users)
    }

    @Test
    fun setRole_whenTargetIsRoot_throwsBadRequest() {
        val actorId = UUID.randomUUID()
        val principal = UserPrincipal(userId = actorId, email = "actor@example.com", role = UserRole.TEACHER)
        val auth = authWithPrincipal(principal)

        every { users.findById(actorId) } returns Optional.of(userEntity(userId = actorId, isRoot = true))
        every { users.findByEmail("target@example.com") } returns userEntity(email = "target@example.com", isRoot = true)

        val ex = assertThrows<ResponseStatusException> {
            controller.setRole(auth, SetUserRoleRequest(email = "target@example.com", role = UserRole.STUDENT))
        }
        assertEquals(HttpStatus.BAD_REQUEST, ex.statusCode)
        assertTrue(ex.reason!!.contains("Cannot change root user role"))

        verify(exactly = 1) { users.findById(actorId) }
        verify(exactly = 1) { users.findByEmail("target@example.com") }
        confirmVerified(users)
    }

    @Test
    fun setRole_whenUpdateReturnsZero_throwsNotFound() {
        val actorId = UUID.randomUUID()
        val principal = UserPrincipal(userId = actorId, email = "actor@example.com", role = UserRole.TEACHER)
        val auth = authWithPrincipal(principal)

        val target = userEntity(email = "target@example.com", isRoot = false)
        every { users.findById(actorId) } returns Optional.of(userEntity(userId = actorId, isRoot = true))
        every { users.findByEmail("target@example.com") } returns target
        every { users.updateRoleByUserId(target.userId, UserRole.TEACHER) } returns 0

        val ex = assertThrows<ResponseStatusException> {
            controller.setRole(auth, SetUserRoleRequest(email = "target@example.com", role = UserRole.TEACHER))
        }
        assertEquals(HttpStatus.NOT_FOUND, ex.statusCode)

        verify(exactly = 1) { users.findById(actorId) }
        verify(exactly = 1) { users.findByEmail("target@example.com") }
        verify(exactly = 1) { users.updateRoleByUserId(target.userId, UserRole.TEACHER) }
        confirmVerified(users)
    }

    @Test
    fun setRole_success_updatesRole() {
        val actorId = UUID.randomUUID()
        val principal = UserPrincipal(userId = actorId, email = "actor@example.com", role = UserRole.TEACHER)
        val auth = authWithPrincipal(principal)

        val target = userEntity(email = "target@example.com", isRoot = false)
        every { users.findById(actorId) } returns Optional.of(userEntity(userId = actorId, isRoot = true))
        every { users.findByEmail("target@example.com") } returns target
        every { users.updateRoleByUserId(target.userId, UserRole.TEACHER) } returns 1

        controller.setRole(auth, SetUserRoleRequest(email = "target@example.com", role = UserRole.TEACHER))

        verify(exactly = 1) { users.findById(actorId) }
        verify(exactly = 1) { users.findByEmail("target@example.com") }
        verify(exactly = 1) { users.updateRoleByUserId(target.userId, UserRole.TEACHER) }
        confirmVerified(users)
    }

    @Test
    fun setActive_whenDeactivatingRoot_throwsBadRequest() {
        val actorId = UUID.randomUUID()
        val principal = UserPrincipal(userId = actorId, email = "actor@example.com", role = UserRole.TEACHER)
        val auth = authWithPrincipal(principal)

        every { users.findById(actorId) } returns Optional.of(userEntity(userId = actorId, isRoot = true))
        every { users.findByEmail("root@example.com") } returns userEntity(email = "root@example.com", isRoot = true)

        val ex = assertThrows<ResponseStatusException> {
            controller.setActive(auth, SetUserActiveRequest(email = "root@example.com", isActive = false))
        }
        assertEquals(HttpStatus.BAD_REQUEST, ex.statusCode)
        assertTrue(ex.reason!!.contains("Cannot deactivate root user"))

        verify(exactly = 1) { users.findById(actorId) }
        verify(exactly = 1) { users.findByEmail("root@example.com") }
        confirmVerified(users)
    }

    @Test
    fun setActive_whenUpdateReturnsZero_throwsNotFound() {
        val actorId = UUID.randomUUID()
        val principal = UserPrincipal(userId = actorId, email = "actor@example.com", role = UserRole.TEACHER)
        val auth = authWithPrincipal(principal)

        val target = userEntity(email = "target@example.com", isRoot = false)
        every { users.findById(actorId) } returns Optional.of(userEntity(userId = actorId, isRoot = true))
        every { users.findByEmail("target@example.com") } returns target
        every { users.updateActiveByUserId(target.userId, false) } returns 0

        val ex = assertThrows<ResponseStatusException> {
            controller.setActive(auth, SetUserActiveRequest(email = "target@example.com", isActive = false))
        }
        assertEquals(HttpStatus.NOT_FOUND, ex.statusCode)

        verify(exactly = 1) { users.findById(actorId) }
        verify(exactly = 1) { users.findByEmail("target@example.com") }
        verify(exactly = 1) { users.updateActiveByUserId(target.userId, false) }
        confirmVerified(users)
    }

    @Test
    fun setActive_whenDeactivatingUser_revokesAllTokens() {
        val actorId = UUID.randomUUID()
        val principal = UserPrincipal(userId = actorId, email = "actor@example.com", role = UserRole.TEACHER)
        val auth = authWithPrincipal(principal)

        val target = userEntity(email = "target@example.com", isRoot = false)
        every { users.findById(actorId) } returns Optional.of(userEntity(userId = actorId, isRoot = true))
        every { users.findByEmail("target@example.com") } returns target
        every { users.updateActiveByUserId(target.userId, false) } returns 1
        every { refreshTokens.revokeAllByUserId(target.userId, any()) } returns 1

        controller.setActive(auth, SetUserActiveRequest(email = "target@example.com", isActive = false))

        verify(exactly = 1) { users.findById(actorId) }
        verify(exactly = 1) { users.findByEmail("target@example.com") }
        verify(exactly = 1) { users.updateActiveByUserId(target.userId, false) }
        verify(exactly = 1) { refreshTokens.revokeAllByUserId(target.userId, any<Instant>()) }
        confirmVerified(users, refreshTokens)
    }

    @Test
    fun setActive_whenActivatingUser_doesNotRevokeTokens() {
        val actorId = UUID.randomUUID()
        val principal = UserPrincipal(userId = actorId, email = "actor@example.com", role = UserRole.TEACHER)
        val auth = authWithPrincipal(principal)

        val target = userEntity(email = "target@example.com", isRoot = false)
        every { users.findById(actorId) } returns Optional.of(userEntity(userId = actorId, isRoot = true))
        every { users.findByEmail("target@example.com") } returns target
        every { users.updateActiveByUserId(target.userId, true) } returns 1

        controller.setActive(auth, SetUserActiveRequest(email = "target@example.com", isActive = true))

        verify(exactly = 1) { users.findById(actorId) }
        verify(exactly = 1) { users.findByEmail("target@example.com") }
        verify(exactly = 1) { users.updateActiveByUserId(target.userId, true) }
        verify(exactly = 0) { refreshTokens.revokeAllByUserId(any(), any()) }
        confirmVerified(users, refreshTokens)
    }

    private fun authWithPrincipal(principal: UserPrincipal): Authentication =
        mockk<Authentication> {
            every { this@mockk.principal } returns principal
        }

    private fun userEntity(
        userId: UUID = UUID.randomUUID(),
        email: String = "user@example.com",
        role: UserRole = UserRole.STUDENT,
        isRoot: Boolean = false,
    ): UserEntity =
        UserEntity(
            userId = userId,
            email = email,
            passwordHash = "hash",
            role = role,
            isRoot = isRoot,
            isActive = true,
            createdAt = Instant.now(),
        )
}
