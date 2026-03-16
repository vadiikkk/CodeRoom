package ru.coderoom.course.repo

import org.springframework.data.jpa.repository.JpaRepository
import ru.coderoom.course.domain.CodeSubmissionAttemptEntity
import java.util.UUID

interface CodeSubmissionAttemptRepository : JpaRepository<CodeSubmissionAttemptEntity, UUID> {
    fun findAllByAssignmentIdOrderByAttemptNumberAsc(assignmentId: UUID): List<CodeSubmissionAttemptEntity>

    fun findAllByAssignmentIdAndStudentUserIdOrderByAttemptNumberAsc(
        assignmentId: UUID,
        studentUserId: UUID,
    ): List<CodeSubmissionAttemptEntity>

    fun findTopByAssignmentIdAndStudentUserIdOrderByAttemptNumberDesc(
        assignmentId: UUID,
        studentUserId: UUID,
    ): CodeSubmissionAttemptEntity?

    fun findAllByCourseId(courseId: UUID): List<CodeSubmissionAttemptEntity>
}
