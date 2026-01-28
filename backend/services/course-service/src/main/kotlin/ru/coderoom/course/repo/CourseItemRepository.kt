package ru.coderoom.course.repo

import org.springframework.data.jpa.repository.JpaRepository
import ru.coderoom.course.domain.CourseItemEntity
import java.util.UUID

interface CourseItemRepository : JpaRepository<CourseItemEntity, UUID> {
    fun findAllByCourseIdOrderByPositionAsc(courseId: UUID): List<CourseItemEntity>
    fun findAllByCourseIdAndBlockIdOrderByPositionAsc(courseId: UUID, blockId: UUID?): List<CourseItemEntity>
    fun existsByItemIdAndCourseId(itemId: UUID, courseId: UUID): Boolean
}
