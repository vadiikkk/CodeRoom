package ru.coderoom.identity.auth

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

interface RefreshTokenRepository : JpaRepository<RefreshTokenEntity, UUID> {
    fun findByTokenHash(tokenHash: String): RefreshTokenEntity?

    @Modifying
    @Transactional
    @Query("update RefreshTokenEntity rt set rt.revokedAt = :now where rt.userId = :userId and rt.revokedAt is null")
    fun revokeAllByUserId(@Param("userId") userId: UUID, @Param("now") now: Instant): Int
}
