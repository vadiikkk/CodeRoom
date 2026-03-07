package ru.coderoom.course.repo

import org.springframework.data.jpa.repository.JpaRepository
import ru.coderoom.course.domain.MaterialEntity
import java.util.UUID

interface MaterialRepository : JpaRepository<MaterialEntity, UUID> {
    fun findAllByCourseIdOrderByCreatedAtAsc(courseId: UUID): List<MaterialEntity>
    fun findByMaterialIdAndCourseId(materialId: UUID, courseId: UUID): MaterialEntity?
    fun existsByMaterialIdAndCourseId(materialId: UUID, courseId: UUID): Boolean
}
