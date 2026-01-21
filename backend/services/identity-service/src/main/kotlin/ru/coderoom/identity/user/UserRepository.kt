package ru.coderoom.identity.user

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface UserRepository : JpaRepository<UserEntity, UUID> {
    fun findByEmail(email: String): UserEntity?
    fun existsByEmail(email: String): Boolean
    fun findByIsRootTrue(): UserEntity?
    fun findByEmailContainingIgnoreCase(email: String, pageable: Pageable): Page<UserEntity>

    @Modifying
    @Query("update UserEntity u set u.role = :role where u.userId = :userId")
    fun updateRoleByUserId(@Param("userId") userId: UUID, @Param("role") role: UserRole): Int

    @Modifying
    @Query("update UserEntity u set u.isActive = :isActive where u.userId = :userId")
    fun updateActiveByUserId(@Param("userId") userId: UUID, @Param("isActive") isActive: Boolean): Int
}
