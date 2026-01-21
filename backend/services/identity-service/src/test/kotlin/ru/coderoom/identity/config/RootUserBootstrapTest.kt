package ru.coderoom.identity.config

import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.boot.ApplicationArguments
import org.springframework.security.crypto.password.PasswordEncoder
import ru.coderoom.identity.user.UserEntity
import ru.coderoom.identity.user.UserRepository
import ru.coderoom.identity.user.UserRole
import java.time.Instant

class RootUserBootstrapTest {
    @Test
    fun whenRootUserAlreadyExists_doesNothing() {
        val props = BootstrapProperties(email = "root@example.com", password = "password123")
        val users = mockk<UserRepository>()
        val passwordEncoder = mockk<PasswordEncoder>()
        val runner = RootUserBootstrap(props, users, passwordEncoder)

        every { users.findByIsRootTrue() } returns userEntity(isRoot = true)

        runner.run(mockk<ApplicationArguments>(relaxed = true))

        verify(exactly = 1) { users.findByIsRootTrue() }
        verify(exactly = 0) { users.save(any()) }
        confirmVerified(users)
    }

    @Test
    fun whenRootUserNotConfigured_throws() {
        val props = BootstrapProperties(email = " ", password = "")
        val users = mockk<UserRepository>()
        val passwordEncoder = mockk<PasswordEncoder>()
        val runner = RootUserBootstrap(props, users, passwordEncoder)

        every { users.findByIsRootTrue() } returns null

        val ex = assertThrows<IllegalStateException> {
            runner.run(mockk<ApplicationArguments>(relaxed = true))
        }

        assertTrue(ex.message!!.contains("Root user is not configured"))
        verify(exactly = 1) { users.findByIsRootTrue() }
        verify(exactly = 0) { users.save(any()) }
        confirmVerified(users)
    }

    @Test
    fun whenRootUserMissing_createsRootUser() {
        val props = BootstrapProperties(email = " ROOT@EXAMPLE.COM ", password = "password123")
        val users = mockk<UserRepository>()
        val passwordEncoder = mockk<PasswordEncoder>()
        val runner = RootUserBootstrap(props, users, passwordEncoder)

        val savedUsers = mutableListOf<UserEntity>()

        every { users.findByIsRootTrue() } returns null
        every { passwordEncoder.encode("password123") } returns "hash"
        every { users.save(capture(savedUsers)) } answers { firstArg() }

        runner.run(mockk<ApplicationArguments>(relaxed = true))

        assertEquals(1, savedUsers.size)
        val saved = savedUsers.single()
        assertEquals("root@example.com", saved.email)
        assertEquals(UserRole.TEACHER, saved.role)
        assertTrue(saved.isRoot)
        assertTrue(saved.isActive)

        verify(exactly = 1) { users.findByIsRootTrue() }
        verify(exactly = 1) { passwordEncoder.encode("password123") }
        verify(exactly = 1) { users.save(any()) }
        confirmVerified(users, passwordEncoder)
    }

    private fun userEntity(isRoot: Boolean): UserEntity =
        UserEntity(
            userId = java.util.UUID.randomUUID(),
            email = "root@example.com",
            passwordHash = "hash",
            role = UserRole.TEACHER,
            isRoot = isRoot,
            isActive = true,
            createdAt = Instant.now(),
        )
}
