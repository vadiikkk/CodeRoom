package ru.coderoom.course.repo

import org.springframework.data.jpa.repository.JpaRepository
import ru.coderoom.course.domain.SubmissionEntity
import java.util.UUID

interface SubmissionRepository : JpaRepository<SubmissionEntity, UUID> {
    fun findAllByAssignmentIdOrderBySubmittedAtAsc(assignmentId: UUID): List<SubmissionEntity>
    fun findAllByCourseIdOrderBySubmittedAtAsc(courseId: UUID): List<SubmissionEntity>
    fun findByAssignmentIdAndOwnerUserId(assignmentId: UUID, ownerUserId: UUID): SubmissionEntity?
    fun findByAssignmentIdAndOwnerGroupId(assignmentId: UUID, ownerGroupId: UUID): SubmissionEntity?
    fun existsByOwnerGroupId(ownerGroupId: UUID): Boolean
}
