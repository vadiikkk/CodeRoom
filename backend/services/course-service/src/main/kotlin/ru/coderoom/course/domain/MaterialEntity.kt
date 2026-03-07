package ru.coderoom.course.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "course_materials")
class MaterialEntity(
    @Id
    @Column(name = "material_id", nullable = false)
    val materialId: UUID,

    @Column(name = "course_id", nullable = false)
    val courseId: UUID,

    @Column(name = "item_id", nullable = false)
    val itemId: UUID,

    @Column(name = "title", nullable = false, length = 200)
    var title: String,

    @Column(name = "description", columnDefinition = "text")
    var description: String? = null,

    @Column(name = "body", columnDefinition = "text")
    var body: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
