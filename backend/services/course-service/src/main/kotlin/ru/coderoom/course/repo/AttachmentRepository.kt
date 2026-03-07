package ru.coderoom.course.repo

import org.springframework.data.jpa.repository.JpaRepository
import ru.coderoom.course.domain.AttachmentEntity
import java.util.UUID

interface AttachmentRepository : JpaRepository<AttachmentEntity, UUID> {
    fun findAllByMaterialIdOrderBySortOrderAscCreatedAtAsc(materialId: UUID): List<AttachmentEntity>
    fun findAllByAssignmentIdOrderBySortOrderAscCreatedAtAsc(assignmentId: UUID): List<AttachmentEntity>
    fun findAllByAttachmentIdIn(attachmentIds: Collection<UUID>): List<AttachmentEntity>
}
