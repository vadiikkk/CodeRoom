package ru.coderoom.identity.auth

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.coderoom.identity.user.UserRole
import java.util.UUID

class JwtServiceTest {
    @Test
    fun issueAccessToken_andParseClaims_roundTrip() {
        val props = JwtProperties(
            secret = "01234567890123456789012345678901",
            accessTokenTtlSeconds = 3600,
            refreshTokenTtlSeconds = 3600,
        )
        val jwtService = JwtService(props)

        val userId = UUID.randomUUID()
        val token = jwtService.issueAccessToken(userId, "user@example.com", UserRole.STUDENT)
        assertTrue(token.isNotBlank())

        val claims = jwtService.parseClaims(token)
        assertEquals(userId.toString(), claims.subject)
        assertEquals("user@example.com", claims["email"])
        assertEquals(UserRole.STUDENT.name, claims["role"])
        assertNotNull(claims.issuedAt)
        assertNotNull(claims.expiration)
        assertTrue(claims.expiration.after(claims.issuedAt))
    }
}
