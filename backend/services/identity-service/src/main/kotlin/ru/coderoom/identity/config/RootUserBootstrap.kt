package ru.coderoom.identity.config

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import ru.coderoom.identity.user.UserEntity
import ru.coderoom.identity.user.UserRepository
import ru.coderoom.identity.user.UserRole
import java.time.Instant
import java.util.UUID

@Component
class RootUserBootstrap(
    private val props: BootstrapProperties,
    private val users: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) : ApplicationRunner {
    override fun run(args: ApplicationArguments) {
        if (users.findByIsRootTrue() != null) return

        val email = props.email.trim().lowercase()
        val password = props.password

        if (email.isBlank() || password.isBlank()) {
            throw IllegalStateException(
                "Root user is not configured. Set coderoom.bootstrap.root.email/password " +
                    "(env: CODEROOM_BOOTSTRAP_ROOT_EMAIL / CODEROOM_BOOTSTRAP_ROOT_PASSWORD).",
            )
        }

        users.save(
            UserEntity(
                userId = UUID.randomUUID(),
                email = email,
                passwordHash = passwordEncoder.encode(password),
                role = UserRole.TEACHER,
                isRoot = true,
                isActive = true,
                createdAt = Instant.now(),
            ),
        )
    }
}
