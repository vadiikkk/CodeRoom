package ru.coderoom.course.repo

import org.springframework.data.jpa.repository.JpaRepository
import ru.coderoom.course.domain.SubmissionMemberEntity
import ru.coderoom.course.domain.SubmissionMemberId
import java.util.UUID

interface SubmissionMemberRepository : JpaRepository<SubmissionMemberEntity, SubmissionMemberId> {
    fun findAllBySubmissionId(submissionId: UUID): List<SubmissionMemberEntity>
    fun findAllByUserId(userId: UUID): List<SubmissionMemberEntity>
    fun existsBySubmissionIdAndUserId(submissionId: UUID, userId: UUID): Boolean
    fun deleteAllBySubmissionId(submissionId: UUID)
}
