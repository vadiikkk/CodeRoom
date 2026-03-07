package ru.coderoom.course.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "course_assignments")
class AssignmentEntity(
    @Id
    @Column(name = "assignment_id", nullable = false)
    val assignmentId: UUID,

    @Column(name = "course_id", nullable = false)
    val courseId: UUID,

    @Column(name = "item_id", nullable = false)
    val itemId: UUID,

    @Column(name = "title", nullable = false, length = 200)
    var title: String,

    @Column(name = "description", columnDefinition = "text")
    var description: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_type", nullable = false, length = 20)
    var assignmentType: AssignmentType,

    @Enumerated(EnumType.STRING)
    @Column(name = "work_type", nullable = false, length = 20)
    var workType: AssignmentWorkType,

    @Column(name = "deadline_at")
    var deadlineAt: Instant? = null,

    @Column(name = "weight", nullable = false, precision = 10, scale = 2)
    var weight: BigDecimal,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
