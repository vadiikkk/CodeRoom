package ru.coderoom.identity.auth

import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.coderoom.identity.auth.dto.AuthResponse
import ru.coderoom.identity.auth.dto.LoginRequest
import ru.coderoom.identity.auth.dto.RefreshRequest
import ru.coderoom.identity.auth.dto.RegisterRequest

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthService,
) {
    @PostMapping("/register")
    fun register(@RequestBody @Valid req: RegisterRequest): ResponseEntity<AuthResponse> {
        val tokens = authService.register(req.email, req.password)
        return ResponseEntity.ok(AuthResponse(accessToken = tokens.accessToken, refreshToken = tokens.refreshToken))
    }

    @PostMapping("/login")
    fun login(@RequestBody @Valid req: LoginRequest): ResponseEntity<AuthResponse> {
        val tokens = authService.login(req.email, req.password)
        return ResponseEntity.ok(AuthResponse(accessToken = tokens.accessToken, refreshToken = tokens.refreshToken))
    }

    @PostMapping("/refresh")
    fun refresh(@RequestBody @Valid req: RefreshRequest): ResponseEntity<AuthResponse> {
        val tokens = authService.refresh(req.refreshToken)
        return ResponseEntity.ok(AuthResponse(accessToken = tokens.accessToken, refreshToken = tokens.refreshToken))
    }

    @PostMapping("/logout")
    fun logout(@RequestBody @Valid req: RefreshRequest): ResponseEntity<Void> {
        authService.logout(req.refreshToken)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/logout-all")
    fun logoutAll(@RequestBody @Valid req: RefreshRequest): ResponseEntity<Void> {
        authService.logoutAll(req.refreshToken)
        return ResponseEntity.noContent().build()
    }
}
