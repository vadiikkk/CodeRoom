package ru.coderoom.identity.user

import jakarta.validation.Valid
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.coderoom.identity.auth.AuthService
import ru.coderoom.identity.auth.UserPrincipal
import ru.coderoom.identity.auth.dto.ChangePasswordRequest
import java.util.UUID

data class MeResponse(
    val userId: UUID,
    val email: String,
    val role: UserRole,
)

@RestController
@RequestMapping("/api/v1/me")
class MeController(
    private val authService: AuthService,
) {
    @GetMapping
    fun me(auth: Authentication): MeResponse {
        val principal = auth.principal as UserPrincipal
        return MeResponse(
            userId = principal.userId,
            email = principal.email,
            role = principal.role,
        )
    }

    @PostMapping("/password")
    fun changePassword(auth: Authentication, @RequestBody @Valid req: ChangePasswordRequest) {
        val principal = auth.principal as UserPrincipal
        authService.changePassword(principal.userId, req.oldPassword, req.newPassword)
    }
}
