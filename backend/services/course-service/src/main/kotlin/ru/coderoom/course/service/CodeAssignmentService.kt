package ru.coderoom.course.service

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import ru.coderoom.course.api.dto.CreateCodeAssignmentRequest
import ru.coderoom.course.api.dto.UpdateCodeAssignmentRequest
import ru.coderoom.course.code.CoderoomConfigMapper
import ru.coderoom.course.domain.AssignmentType
import ru.coderoom.course.domain.AssignmentWorkType
import ru.coderoom.course.domain.AttachmentEntity
import ru.coderoom.course.domain.CodeAssignmentEntity
import ru.coderoom.course.github.GithubClient
import ru.coderoom.course.repo.AssignmentRepository
import ru.coderoom.course.repo.AttachmentRepository
import ru.coderoom.course.repo.CodeAssignmentRepository
import ru.coderoom.course.repo.CourseItemRepository
import ru.coderoom.course.repo.SubmissionAttachmentRepository
import java.time.Instant
import java.util.UUID

@Service
class CodeAssignmentService(
    private val access: CourseAccessService,
    private val app: CourseAppService,
    private val content: CourseContentService,
    private val assignments: AssignmentRepository,
    private val codeAssignments: CodeAssignmentRepository,
    private val items: CourseItemRepository,
    private val attachments: AttachmentRepository,
    private val submissionAttachments: SubmissionAttachmentRepository,
    private val github: GithubClient,
    private val configMapper: CoderoomConfigMapper,
) {
    @Transactional
    fun createAssignment(
        courseId: UUID,
        userId: UUID,
        title: String,
        description: String?,
        workType: AssignmentWorkType,
        deadlineAt: java.time.Instant?,
        weight: java.math.BigDecimal,
        blockId: UUID?,
        position: Int,
        isVisible: Boolean,
        attachmentIds: List<UUID>,
        code: CreateCodeAssignmentRequest,
    ): CodeAssignmentAggregate {
        if (workType != AssignmentWorkType.INDIVIDUAL) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "CODE assignments currently support only INDIVIDUAL workType")
        }
        val pat = app.resolveGithubPatForTeacher(courseId, userId, code.githubPat)
        val privateTestsAttachment = validatePrivateTestsAttachment(courseId, code.privateTestsAttachmentId)
        val starterConfig = configMapper.createDefault(
            language = code.language,
            maxAttempts = code.maxAttempts,
            privateTestsEnabled = privateTestsAttachment != null,
        )
        val aggregate = content.createAssignment(
            courseId = courseId,
            userId = userId,
            title = title,
            description = description,
            assignmentType = AssignmentType.CODE,
            workType = workType,
            deadlineAt = deadlineAt,
            weight = weight,
            blockId = blockId,
            position = position,
            isVisible = isVisible,
            attachmentIds = attachmentIds,
        )
        val repo = github.createPrivateRepository(
            token = pat,
            name = code.repositoryName.trim(),
            description = code.repositoryDescription?.trim()?.ifBlank { null } ?: description,
        )
        github.createOrUpdateFile(
            token = pat,
            repoFullName = repo.fullName,
            path = ".coderoom.yml",
            commitMessage = "Initialize CodeRoom autograding config",
            content = starterConfig,
            branch = repo.defaultBranch,
        )
        val now = Instant.now()
        val codeAssignment = codeAssignments.save(
            CodeAssignmentEntity(
                assignmentId = aggregate.assignment.assignmentId,
                language = code.language,
                repositoryName = repo.name,
                repositoryFullName = repo.fullName,
                repositoryUrl = repo.htmlUrl,
                defaultBranch = repo.defaultBranch,
                maxAttempts = code.maxAttempts,
                privateTestsAttachmentId = privateTestsAttachment?.attachmentId,
                repositoryPrivate = repo.private,
                starterConfig = starterConfig,
                createdAt = now,
                updatedAt = now,
            ),
        )
        return CodeAssignmentAggregate(
            assignment = aggregate,
            codeAssignment = codeAssignment,
            privateTestsAttachment = privateTestsAttachment,
        )
    }

    @Transactional(readOnly = true)
    fun getCodeAssignmentForRead(assignmentId: UUID, userId: UUID): CodeAssignmentAggregate {
        val codeAssignment = codeAssignments.findById(assignmentId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Code assignment not found")
        }
        val base = content.getAssignment(assignmentId, userId)
        val privateTests =
            codeAssignment.privateTestsAttachmentId?.let { attachments.findById(it).orElse(null) }
        return CodeAssignmentAggregate(base, codeAssignment, privateTests)
    }

    @Transactional(readOnly = true)
    fun findCodeAssignmentExtras(assignmentId: UUID): CodeAssignmentExtras? {
        val codeAssignment = codeAssignments.findById(assignmentId).orElse(null) ?: return null
        val privateTests = codeAssignment.privateTestsAttachmentId?.let { attachments.findById(it).orElse(null) }
        return CodeAssignmentExtras(codeAssignment = codeAssignment, privateTestsAttachment = privateTests)
    }

    @Transactional
    fun updateCodeAssignment(
        assignmentId: UUID,
        userId: UUID,
        title: String?,
        description: String?,
        deadlineAt: java.time.Instant?,
        clearDeadline: Boolean,
        weight: java.math.BigDecimal?,
        blockId: UUID?,
        clearBlock: Boolean,
        position: Int?,
        isVisible: Boolean?,
        attachmentIds: List<UUID>?,
        code: UpdateCodeAssignmentRequest?,
    ): CodeAssignmentAggregate {
        val current = codeAssignments.findById(assignmentId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Code assignment not found")
        }
        val aggregate = content.updateAssignment(
            assignmentId = assignmentId,
            userId = userId,
            title = title,
            description = description,
            assignmentType = AssignmentType.CODE,
            workType = AssignmentWorkType.INDIVIDUAL,
            deadlineAt = deadlineAt,
            clearDeadline = clearDeadline,
            weight = weight,
            blockId = blockId,
            clearBlock = clearBlock,
            position = position,
            isVisible = isVisible,
            attachmentIds = attachmentIds,
        )
        var privateTestsAttachment: AttachmentEntity? =
            current.privateTestsAttachmentId?.let { attachments.findById(it).orElse(null) }
        if (code != null) {
            code.maxAttempts?.let { current.maxAttempts = it }
            when {
                code.clearPrivateTestsAttachment -> {
                    current.privateTestsAttachmentId = null
                    privateTestsAttachment = null
                }
                code.privateTestsAttachmentId != null -> {
                    privateTestsAttachment = validatePrivateTestsAttachment(aggregate.assignment.courseId, code.privateTestsAttachmentId)
                    current.privateTestsAttachmentId = privateTestsAttachment?.attachmentId
                }
            }
            current.updatedAt = Instant.now()
            codeAssignments.save(current)
        }
        return CodeAssignmentAggregate(
            assignment = aggregate,
            codeAssignment = current,
            privateTestsAttachment = privateTestsAttachment,
        )
    }

    @Transactional
    fun publishAssignment(assignmentId: UUID, userId: UUID): CodeAssignmentAggregate {
        val assignment = assignments.findById(assignmentId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found")
        }
        access.requireTeacher(assignment.courseId, userId)
        val codeAssignment = codeAssignments.findById(assignmentId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Code assignment not found")
        }
        val pat = app.requireGithubPat(assignment.courseId, userId)
        val updatedRepo = github.makeRepositoryPublic(pat, codeAssignment.repositoryFullName)
        val item = items.findById(assignment.itemId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment item not found")
        }
        val now = Instant.now()
        item.isVisible = true
        item.updatedAt = now
        assignment.updatedAt = now
        codeAssignment.repositoryPrivate = updatedRepo.private
        codeAssignment.publishedAt = now
        codeAssignment.updatedAt = now
        items.save(item)
        assignments.save(assignment)
        codeAssignments.save(codeAssignment)
        return CodeAssignmentAggregate(
            assignment = content.getAssignment(assignmentId, userId),
            codeAssignment = codeAssignment,
            privateTestsAttachment = codeAssignment.privateTestsAttachmentId?.let { attachments.findById(it).orElse(null) },
        )
    }

    @Transactional(readOnly = true)
    fun loadCurrentConfigSnapshot(assignmentId: UUID, userId: UUID): String {
        val codeAssignment = codeAssignments.findById(assignmentId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Code assignment not found")
        }
        val assignment = assignments.findById(assignmentId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found")
        }
        val pat = app.requireGithubPat(assignment.courseId, userId)
        val file = github.getFile(
            token = pat,
            repoFullName = codeAssignment.repositoryFullName,
            path = ".coderoom.yml",
            ref = codeAssignment.defaultBranch,
        )
        val content = file.content ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, ".coderoom.yml is empty")
        configMapper.parseAndValidate(content)
        return content
    }

    private fun validatePrivateTestsAttachment(courseId: UUID, attachmentId: UUID?): AttachmentEntity? {
        if (attachmentId == null) return null
        val attachment = attachments.findById(attachmentId).orElseThrow {
            ResponseStatusException(HttpStatus.BAD_REQUEST, "Private tests attachment not found")
        }
        if (attachment.courseId != courseId) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Private tests attachment does not belong to course")
        }
        if (attachment.materialId != null || attachment.assignmentId != null || submissionAttachments.findByAttachmentId(attachmentId) != null) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Private tests attachment must not be linked to content or submissions")
        }
        return attachment
    }
}

data class CodeAssignmentAggregate(
    val assignment: AssignmentAggregate,
    val codeAssignment: CodeAssignmentEntity,
    val privateTestsAttachment: AttachmentEntity?,
)

data class CodeAssignmentExtras(
    val codeAssignment: CodeAssignmentEntity,
    val privateTestsAttachment: AttachmentEntity?,
)
