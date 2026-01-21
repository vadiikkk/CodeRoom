package ru.coderoom.identity.auth

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Date
import java.util.UUID
import ru.coderoom.identity.user.UserRole

@Service
class JwtService(
    private val props: JwtProperties,
) {
    private fun signingKey() =
        Keys.hmacShaKeyFor(props.secret.toByteArray(StandardCharsets.UTF_8))

    fun issueAccessToken(userId: UUID, email: String, role: UserRole): String {
        val now = Instant.now()
        val exp = now.plusSeconds(props.accessTokenTtlSeconds)

        return Jwts.builder()
            .subject(userId.toString())
            .issuedAt(Date.from(now))
            .expiration(Date.from(exp))
            .claim("email", email)
            .claim("role", role.name)
            .signWith(signingKey())
            .compact()
    }

    fun parseClaims(token: String): Claims =
        Jwts.parser()
            .verifyWith(signingKey())
            .build()
            .parseSignedClaims(token)
            .payload
}
