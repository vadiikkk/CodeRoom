package ru.coderoom.course.repo

import org.springframework.data.jpa.repository.JpaRepository
import ru.coderoom.course.domain.AssignmentEntity
import java.util.UUID

interface AssignmentRepository : JpaRepository<AssignmentEntity, UUID> {
    fun findAllByCourseIdOrderByCreatedAtAsc(courseId: UUID): List<AssignmentEntity>
    fun findByAssignmentIdAndCourseId(assignmentId: UUID, courseId: UUID): AssignmentEntity?
    fun existsByAssignmentIdAndCourseId(assignmentId: UUID, courseId: UUID): Boolean
}
