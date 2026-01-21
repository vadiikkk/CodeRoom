package ru.coderoom.identity.auth

import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import ru.coderoom.identity.auth.dto.AuthResponse
import ru.coderoom.identity.auth.dto.LoginRequest
import ru.coderoom.identity.auth.dto.RefreshRequest
import ru.coderoom.identity.auth.dto.RegisterRequest

class AuthControllerTest {
    private val authService = mockk<AuthService>()
    private val controller = AuthController(authService)

    @Test
    fun register_returnsTokensFromService() {
        every { authService.register("user@example.com", "password123") } returns AuthTokens(
            accessToken = "access",
            refreshToken = "refresh",
        )

        val res = controller.register(RegisterRequest(email = "user@example.com", password = "password123"))

        assertEquals(HttpStatus.OK, res.statusCode)
        assertEquals(AuthResponse(accessToken = "access", refreshToken = "refresh"), res.body)
        verify(exactly = 1) { authService.register("user@example.com", "password123") }
        confirmVerified(authService)
    }

    @Test
    fun login_returnsTokensFromService() {
        every { authService.login("user@example.com", "password123") } returns AuthTokens(
            accessToken = "access",
            refreshToken = "refresh",
        )

        val res = controller.login(LoginRequest(email = "user@example.com", password = "password123"))

        assertEquals(HttpStatus.OK, res.statusCode)
        assertEquals(AuthResponse(accessToken = "access", refreshToken = "refresh"), res.body)
        verify(exactly = 1) { authService.login("user@example.com", "password123") }
        confirmVerified(authService)
    }

    @Test
    fun refresh_returnsTokensFromService() {
        every { authService.refresh("refresh") } returns AuthTokens(
            accessToken = "newAccess",
            refreshToken = "newRefresh",
        )

        val res = controller.refresh(RefreshRequest(refreshToken = "refresh"))

        assertEquals(HttpStatus.OK, res.statusCode)
        assertEquals(AuthResponse(accessToken = "newAccess", refreshToken = "newRefresh"), res.body)
        verify(exactly = 1) { authService.refresh("refresh") }
        confirmVerified(authService)
    }

    @Test
    fun logout_revokesProvidedRefreshToken() {
        every { authService.logout("refresh") } returns Unit

        val res = controller.logout(RefreshRequest(refreshToken = "refresh"))

        assertEquals(HttpStatus.NO_CONTENT, res.statusCode)
        verify(exactly = 1) { authService.logout("refresh") }
        confirmVerified(authService)
    }

    @Test
    fun logoutAll_revokesAllSessionsForProvidedRefreshToken() {
        every { authService.logoutAll("refresh") } returns Unit

        val res = controller.logoutAll(RefreshRequest(refreshToken = "refresh"))

        assertEquals(HttpStatus.NO_CONTENT, res.statusCode)
        verify(exactly = 1) { authService.logoutAll("refresh") }
        confirmVerified(authService)
    }
}
