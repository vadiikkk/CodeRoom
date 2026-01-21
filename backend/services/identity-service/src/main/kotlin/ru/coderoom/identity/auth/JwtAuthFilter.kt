package ru.coderoom.identity.auth

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import ru.coderoom.identity.user.UserRepository
import java.util.UUID

@Component
class JwtAuthFilter(
    private val jwtService: JwtService,
    private val users: UserRepository,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val authHeader = request.getHeader("Authorization")
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response)
            return
        }

        val token = authHeader.removePrefix("Bearer ").trim()
        runCatching {
            val claims = jwtService.parseClaims(token)
            val userId = UUID.fromString(claims.subject)
            val user = users.findById(userId).orElse(null) ?: return@runCatching
            if (!user.isActive) return@runCatching

            val principal = UserPrincipal(
                userId = user.userId,
                email = user.email,
                role = user.role,
            )

            val authorities = buildList {
                add(SimpleGrantedAuthority("ROLE_${user.role.name}"))
                if (user.isRoot) add(SimpleGrantedAuthority("ROLE_ROOT"))
            }

            val authentication = UsernamePasswordAuthenticationToken(
                principal, null, authorities,
            ).apply {
                details = WebAuthenticationDetailsSource().buildDetails(request)
            }

            SecurityContextHolder.getContext().authentication = authentication
        }

        filterChain.doFilter(request, response)
    }
}
