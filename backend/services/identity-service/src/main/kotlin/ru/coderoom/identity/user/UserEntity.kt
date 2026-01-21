package ru.coderoom.identity.user

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Id
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "users")
class UserEntity(
    @Id
    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(name = "email", nullable = false, unique = true)
    val email: String,

    @Column(name = "password_hash", nullable = false)
    var passwordHash: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    var role: UserRole,

    @Column(name = "is_root", nullable = false)
    val isRoot: Boolean,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant,
)
