package ru.coderoom.identity.user

import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.security.core.Authentication
import ru.coderoom.identity.auth.AuthService
import ru.coderoom.identity.auth.UserPrincipal
import ru.coderoom.identity.auth.dto.ChangePasswordRequest
import java.util.UUID

class MeControllerTest {
    private val authService = mockk<AuthService>()
    private val controller = MeController(authService)

    @Test
    fun me_returnsPrincipalData() {
        val userId = UUID.randomUUID()
        val principal = UserPrincipal(userId = userId, email = "me@example.com", role = UserRole.TEACHER)
        val auth = authWithPrincipal(principal)

        val res = controller.me(auth)

        assertEquals(userId, res.userId)
        assertEquals("me@example.com", res.email)
        assertEquals(UserRole.TEACHER, res.role)
        verify(exactly = 1) { auth.principal }
        confirmVerified(auth)
    }

    @Test
    fun changePassword_callsAuthService() {
        val userId = UUID.randomUUID()
        val principal = UserPrincipal(userId = userId, email = "me@example.com", role = UserRole.STUDENT)
        val auth = authWithPrincipal(principal)

        every { authService.changePassword(userId, "old", "new") } returns Unit

        controller.changePassword(auth, ChangePasswordRequest(oldPassword = "old", newPassword = "new"))

        verify(exactly = 1) { auth.principal }
        verify(exactly = 1) { authService.changePassword(userId, "old", "new") }
        confirmVerified(auth, authService)
    }

    private fun authWithPrincipal(principal: UserPrincipal): Authentication =
        mockk<Authentication> {
            every { this@mockk.principal } returns principal
        }
}
