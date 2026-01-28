package ru.coderoom.course.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets

@Service
class JwtService(
    private val props: JwtProperties,
) {
    private fun signingKey() =
        Keys.hmacShaKeyFor(props.secret.toByteArray(StandardCharsets.UTF_8))

    fun parseClaims(token: String): Claims =
        Jwts.parser()
            .verifyWith(signingKey())
            .build()
            .parseSignedClaims(token)
            .payload
}
