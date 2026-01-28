package ru.coderoom.course.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

@Component
class GatewayOrJwtAuthFilter(
    private val jwtService: JwtService,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        if (SecurityContextHolder.getContext().authentication != null) {
            filterChain.doFilter(request, response)
            return
        }

        val fromJwt = extractFromJwt(request)
        val fromGatewayHeaders = extractFromGatewayHeaders(request)
        val user = fromJwt ?: fromGatewayHeaders

        if (user != null) {
            val auth = UsernamePasswordAuthenticationToken(
                user,
                null,
                listOf(SimpleGrantedAuthority("ROLE_${user.role.name}")),
            ).apply {
                details = WebAuthenticationDetailsSource().buildDetails(request)
            }
            SecurityContextHolder.getContext().authentication = auth
        }

        filterChain.doFilter(request, response)
    }

    private fun extractFromJwt(request: HttpServletRequest): RequestUser? {
        val authHeader = request.getHeader("Authorization") ?: return null
        if (!authHeader.startsWith("Bearer ")) return null

        val token = authHeader.removePrefix("Bearer ").trim()
        return runCatching {
            val claims = jwtService.parseClaims(token)
            val userId = UUID.fromString(claims.subject)
            val roleRaw = (claims["role"] as? String) ?: return@runCatching null
            val role = GlobalRole.valueOf(roleRaw)
            RequestUser(userId = userId, role = role)
        }.getOrNull()
    }

    private fun extractFromGatewayHeaders(request: HttpServletRequest): RequestUser? {
        val userIdHeader = request.getHeader("X-User-Id") ?: return null
        val roleHeader = request.getHeader("X-User-Role") ?: return null

        return runCatching {
            RequestUser(
                userId = UUID.fromString(userIdHeader),
                role = GlobalRole.valueOf(roleHeader),
            )
        }.getOrNull()
    }
}
