package ru.coderoom.course.repo

import org.springframework.data.jpa.repository.JpaRepository
import ru.coderoom.course.domain.CourseGroupEntity
import java.util.UUID

interface CourseGroupRepository : JpaRepository<CourseGroupEntity, UUID> {
    fun findAllByCourseIdOrderByCreatedAtAsc(courseId: UUID): List<CourseGroupEntity>
    fun existsByGroupIdAndCourseId(groupId: UUID, courseId: UUID): Boolean
    fun existsByCourseIdAndNameIgnoreCase(courseId: UUID, name: String): Boolean
}
