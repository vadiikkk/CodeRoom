package ru.coderoom.course.repo

import org.springframework.data.jpa.repository.JpaRepository
import ru.coderoom.course.domain.SubmissionAttachmentEntity
import ru.coderoom.course.domain.SubmissionAttachmentId
import java.util.UUID

interface SubmissionAttachmentRepository : JpaRepository<SubmissionAttachmentEntity, SubmissionAttachmentId> {
    fun findAllBySubmissionIdOrderBySortOrderAscCreatedAtAsc(submissionId: UUID): List<SubmissionAttachmentEntity>
    fun findByAttachmentId(attachmentId: UUID): SubmissionAttachmentEntity?
    fun deleteAllBySubmissionId(submissionId: UUID)
}
