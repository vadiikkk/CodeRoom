package ru.coderoom.course.repo

import org.springframework.data.jpa.repository.JpaRepository
import ru.coderoom.course.domain.CourseBlockEntity
import java.util.UUID

interface CourseBlockRepository : JpaRepository<CourseBlockEntity, UUID> {
    fun findAllByCourseIdOrderByPositionAsc(courseId: UUID): List<CourseBlockEntity>
    fun existsByBlockIdAndCourseId(blockId: UUID, courseId: UUID): Boolean
}
