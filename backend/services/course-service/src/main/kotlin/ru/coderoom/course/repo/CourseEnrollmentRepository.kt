package ru.coderoom.course.repo

import org.springframework.data.jpa.repository.JpaRepository
import ru.coderoom.course.domain.CourseEnrollmentEntity
import ru.coderoom.course.domain.CourseEnrollmentId
import java.util.UUID

interface CourseEnrollmentRepository : JpaRepository<CourseEnrollmentEntity, CourseEnrollmentId> {
    fun findByCourseIdAndUserId(courseId: UUID, userId: UUID): CourseEnrollmentEntity?
    fun findAllByCourseId(courseId: UUID): List<CourseEnrollmentEntity>
    fun findAllByUserId(userId: UUID): List<CourseEnrollmentEntity>
    fun existsByCourseIdAndUserId(courseId: UUID, userId: UUID): Boolean
}
