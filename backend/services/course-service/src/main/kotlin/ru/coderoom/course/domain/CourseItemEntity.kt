package ru.coderoom.course.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "course_items")
class CourseItemEntity(
    @Id
    @Column(name = "item_id", nullable = false)
    val itemId: UUID,

    @Column(name = "course_id", nullable = false)
    val courseId: UUID,

    @Column(name = "block_id")
    var blockId: UUID? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 20)
    var itemType: CourseItemType,

    @Column(name = "ref_id", nullable = false)
    var refId: UUID,

    @Column(name = "position", nullable = false)
    var position: Int,

    @Column(name = "is_visible", nullable = false)
    var isVisible: Boolean = true,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
