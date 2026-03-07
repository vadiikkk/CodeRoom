package ru.coderoom.course.service

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import ru.coderoom.course.domain.AssignmentType
import ru.coderoom.course.domain.AssignmentWorkType
import ru.coderoom.course.domain.AttachmentEntity
import ru.coderoom.course.domain.CourseGroupEntity
import ru.coderoom.course.domain.RoleInCourse
import ru.coderoom.course.domain.SubmissionAttachmentEntity
import ru.coderoom.course.domain.SubmissionEntity
import ru.coderoom.course.domain.SubmissionGraderType
import ru.coderoom.course.domain.SubmissionMemberEntity
import ru.coderoom.course.domain.SubmissionOwnerType
import ru.coderoom.course.domain.SubmissionStatus
import ru.coderoom.course.repo.AttachmentRepository
import ru.coderoom.course.repo.SubmissionAttachmentRepository
import ru.coderoom.course.repo.SubmissionMemberRepository
import ru.coderoom.course.repo.SubmissionRepository
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Service
class SubmissionService(
    private val access: CourseAccessService,
    private val content: CourseContentService,
    private val groups: CourseGroupService,
    private val attachments: AttachmentRepository,
    private val submissions: SubmissionRepository,
    private val submissionMembers: SubmissionMemberRepository,
    private val submissionAttachments: SubmissionAttachmentRepository,
) {
    @Transactional
    fun createOrUpdateMySubmission(
        assignmentId: UUID,
        userId: UUID,
        textAnswer: String?,
        attachmentIds: List<UUID>,
    ): SubmissionAggregate {
        val assignmentAggregate = content.getAssignment(assignmentId, userId)
        val role = access.requireMember(assignmentAggregate.assignment.courseId, userId)
        if (role != RoleInCourse.STUDENT) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Only students can submit assignments")
        }

        val owner = resolveOwner(assignmentAggregate.assignment.courseId, assignmentAggregate.assignment.workType, userId)
        validateSubmissionContent(assignmentAggregate.assignment.assignmentType, textAnswer, attachmentIds)

        val now = Instant.now()
        val existing = when (owner.ownerType) {
            SubmissionOwnerType.USER -> submissions.findByAssignmentIdAndOwnerUserId(assignmentId, owner.ownerUserId!!)
            SubmissionOwnerType.GROUP -> submissions.findByAssignmentIdAndOwnerGroupId(assignmentId, owner.ownerGroupId!!)
        }

        val submission =
            if (existing == null) {
                submissions.save(
                    SubmissionEntity(
                        submissionId = UUID.randomUUID(),
                        courseId = assignmentAggregate.assignment.courseId,
                        assignmentId = assignmentId,
                        ownerType = owner.ownerType,
                        ownerUserId = owner.ownerUserId,
                        ownerGroupId = owner.ownerGroupId,
                        textAnswer = normalizeText(textAnswer),
                        status = SubmissionStatus.SUBMITTED,
                        submittedAt = now,
                        updatedAt = now,
                    ),
                )
            } else {
                if (existing.status == SubmissionStatus.GRADED) {
                    throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Submission has already been graded")
                }
                existing.textAnswer = normalizeText(textAnswer)
                existing.updatedAt = now
                existing.score = null
                existing.comment = null
                existing.gradedAt = null
                existing.gradedByUserId = null
                existing.graderType = null
                existing.status = SubmissionStatus.SUBMITTED
                submissions.save(existing)
            }

        replaceSubmissionMembers(submission.submissionId, owner.memberUserIds)
        val linkedAttachments = replaceSubmissionAttachments(submission.submissionId, submission.courseId, attachmentIds)
        return toAggregate(submission, owner.group, owner.memberUserIds, linkedAttachments)
    }

    @Transactional(readOnly = true)
    fun listSubmissionsForAssignment(assignmentId: UUID, userId: UUID): List<SubmissionAggregate> {
        val assignment = content.getAssignment(assignmentId, userId).assignment
        access.requireStaff(assignment.courseId, userId)
        val byGroupId = submissions.findAllByAssignmentIdOrderBySubmittedAtAsc(assignmentId)
            .map(::toAggregate)
        return byGroupId
    }

    @Transactional(readOnly = true)
    fun getMySubmissionForAssignment(assignmentId: UUID, userId: UUID): SubmissionAggregate {
        val assignment = content.getAssignment(assignmentId, userId).assignment
        val role = access.requireMember(assignment.courseId, userId)
        if (role != RoleInCourse.STUDENT) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Only students can view personal submissions")
        }
        val owner = resolveOwner(assignment.courseId, assignment.workType, userId)
        val submission = when (owner.ownerType) {
            SubmissionOwnerType.USER -> submissions.findByAssignmentIdAndOwnerUserId(assignmentId, owner.ownerUserId!!)
            SubmissionOwnerType.GROUP -> submissions.findByAssignmentIdAndOwnerGroupId(assignmentId, owner.ownerGroupId!!)
        } ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Submission not found")

        return toAggregate(submission)
    }

    @Transactional(readOnly = true)
    fun getSubmission(submissionId: UUID, userId: UUID): SubmissionAggregate {
        val submission = submissions.findById(submissionId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Submission not found")
        }
        val role = access.requireMember(submission.courseId, userId)
        if (role == RoleInCourse.STUDENT) {
            if (!submissionMembers.existsBySubmissionIdAndUserId(submissionId, userId)) {
                throw ResponseStatusException(HttpStatus.NOT_FOUND, "Submission not found")
            }
            content.getAssignment(submission.assignmentId, userId)
        }
        return toAggregate(submission)
    }

    @Transactional
    fun gradeSubmission(
        submissionId: UUID,
        userId: UUID,
        score: BigDecimal,
        comment: String?,
    ): SubmissionAggregate {
        val submission = submissions.findById(submissionId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Submission not found")
        }
        access.requireStaff(submission.courseId, userId)
        validateScore(score)

        submission.score = score
        submission.comment = comment
        submission.gradedByUserId = userId
        submission.gradedAt = Instant.now()
        submission.graderType = SubmissionGraderType.MANUAL
        submission.status = SubmissionStatus.GRADED
        submission.updatedAt = Instant.now()
        return toAggregate(submissions.save(submission))
    }

    @Transactional(readOnly = true)
    fun listCourseSubmissions(courseId: UUID, userId: UUID): List<SubmissionAggregate> {
        access.requireStaff(courseId, userId)
        return submissions.findAllByCourseIdOrderBySubmittedAtAsc(courseId).map(::toAggregate)
    }

    private fun resolveOwner(courseId: UUID, workType: AssignmentWorkType, userId: UUID): SubmissionOwner {
        return when (workType) {
            AssignmentWorkType.INDIVIDUAL ->
                SubmissionOwner(
                    ownerType = SubmissionOwnerType.USER,
                    ownerUserId = userId,
                    ownerGroupId = null,
                    memberUserIds = listOf(userId),
                    group = null,
                )

            AssignmentWorkType.GROUP -> {
                val groupAggregate = groups.findGroupForStudent(courseId, userId)
                    ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Student must belong to a group")
                SubmissionOwner(
                    ownerType = SubmissionOwnerType.GROUP,
                    ownerUserId = null,
                    ownerGroupId = groupAggregate.group.groupId,
                    memberUserIds = groupAggregate.members.map { it.userId }.sortedBy(UUID::toString),
                    group = groupAggregate.group,
                )
            }
        }
    }

    private fun validateSubmissionContent(assignmentType: AssignmentType, textAnswer: String?, attachmentIds: List<UUID>) {
        when (assignmentType) {
            AssignmentType.TEXT -> {
                if (textAnswer.isNullOrBlank()) {
                    throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Text submission must contain textAnswer")
                }
                if (attachmentIds.isNotEmpty()) {
                    throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Text submission cannot contain attachments")
                }
            }

            AssignmentType.FILE -> {
                if (attachmentIds.isEmpty()) {
                    throw ResponseStatusException(HttpStatus.BAD_REQUEST, "File submission must contain attachments")
                }
            }

            AssignmentType.CODE ->
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "CODE submissions are not supported yet")
        }
    }

    private fun replaceSubmissionMembers(submissionId: UUID, memberUserIds: List<UUID>) {
        submissionMembers.deleteAllBySubmissionId(submissionId)
        memberUserIds.forEach { memberUserId ->
            submissionMembers.save(SubmissionMemberEntity(submissionId = submissionId, userId = memberUserId))
        }
    }

    private fun replaceSubmissionAttachments(
        submissionId: UUID,
        courseId: UUID,
        attachmentIds: List<UUID>,
    ): List<AttachmentEntity> {
        submissionAttachments.deleteAllBySubmissionId(submissionId)
        val uniqueIds = attachmentIds.distinct()
        if (uniqueIds.isEmpty()) return emptyList()

        val found = attachments.findAllByAttachmentIdIn(uniqueIds)
        if (found.size != uniqueIds.size) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Some attachments were not found")
        }

        val ordered = uniqueIds.map { id -> found.first { it.attachmentId == id } }
        ordered.forEach { attachment ->
            if (attachment.courseId != courseId) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Attachment does not belong to course")
            }
            if (attachment.materialId != null || attachment.assignmentId != null) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Attachment is already linked to course content")
            }
            val linkedSubmission = submissionAttachments.findByAttachmentId(attachment.attachmentId)
            if (linkedSubmission != null && linkedSubmission.submissionId != submissionId) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Attachment is already linked to another submission")
            }
        }

        ordered.forEachIndexed { index, attachment ->
            submissionAttachments.save(
                SubmissionAttachmentEntity(
                    submissionId = submissionId,
                    attachmentId = attachment.attachmentId,
                    sortOrder = index,
                    createdAt = Instant.now(),
                ),
            )
        }
        return ordered
    }

    private fun toAggregate(submission: SubmissionEntity): SubmissionAggregate {
        val members = submissionMembers.findAllBySubmissionId(submission.submissionId).map { it.userId }.sortedBy(UUID::toString)
        val links = submissionAttachments.findAllBySubmissionIdOrderBySortOrderAscCreatedAtAsc(submission.submissionId)
        val attachmentsById = attachments.findAllByAttachmentIdIn(links.map { it.attachmentId }).associateBy { it.attachmentId }
        val orderedAttachments = links.mapNotNull { attachmentsById[it.attachmentId] }
        val group = submission.ownerGroupId?.let { groups.requireGroup(submission.courseId, it) }
        return toAggregate(submission, group, members, orderedAttachments)
    }

    private fun toAggregate(
        submission: SubmissionEntity,
        group: CourseGroupEntity?,
        members: List<UUID>,
        attachments: List<AttachmentEntity>,
    ): SubmissionAggregate =
        SubmissionAggregate(
            submission = submission,
            group = group,
            memberUserIds = members,
            attachments = attachments,
        )

    private fun validateScore(score: BigDecimal) {
        if (score < BigDecimal.ZERO || score > BigDecimal("100")) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "score must be between 0 and 100")
        }
    }

    private fun normalizeText(textAnswer: String?): String? =
        textAnswer?.trim()?.ifBlank { null }
}

data class SubmissionAggregate(
    val submission: SubmissionEntity,
    val group: CourseGroupEntity?,
    val memberUserIds: List<UUID>,
    val attachments: List<AttachmentEntity>,
)

private data class SubmissionOwner(
    val ownerType: SubmissionOwnerType,
    val ownerUserId: UUID?,
    val ownerGroupId: UUID?,
    val memberUserIds: List<UUID>,
    val group: CourseGroupEntity?,
)
