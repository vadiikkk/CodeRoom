package ru.coderoom.course.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import java.io.Serializable
import java.util.UUID

data class SubmissionMemberId(
    val submissionId: UUID = UUID(0, 0),
    val userId: UUID = UUID(0, 0),
) : Serializable

@Entity
@Table(name = "submission_members")
@IdClass(SubmissionMemberId::class)
class SubmissionMemberEntity(
    @Id
    @Column(name = "submission_id", nullable = false)
    val submissionId: UUID,

    @Id
    @Column(name = "user_id", nullable = false)
    val userId: UUID,
)
