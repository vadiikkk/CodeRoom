package ru.coderoom.course.service

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import ru.coderoom.course.api.dto.CreateCodeAttemptRequest
import ru.coderoom.course.content.ContentClient
import ru.coderoom.course.content.PresignedUrlResponse
import ru.coderoom.course.code.CoderoomConfigMapper
import ru.coderoom.course.domain.AssignmentType
import ru.coderoom.course.domain.AttachmentEntity
import ru.coderoom.course.domain.CodeSubmissionAttemptEntity
import ru.coderoom.course.domain.CodeSubmissionAttemptStatus
import ru.coderoom.course.domain.RoleInCourse
import ru.coderoom.course.github.GithubClient
import ru.coderoom.course.messaging.AttachedObjectPayload
import ru.coderoom.course.messaging.AutogradeFinishedMessage
import ru.coderoom.course.messaging.AutogradePublisher
import ru.coderoom.course.messaging.AutogradeRequestedMessage
import ru.coderoom.course.repo.AttachmentRepository
import ru.coderoom.course.repo.CodeAssignmentRepository
import ru.coderoom.course.repo.CodeSubmissionAttemptRepository
import java.time.Instant
import java.util.UUID

@Service
class CodeAttemptService(
    private val access: CourseAccessService,
    private val content: CourseContentService,
    private val codeAssignments: CodeAssignmentRepository,
    private val attempts: CodeSubmissionAttemptRepository,
    private val attachments: AttachmentRepository,
    private val github: GithubClient,
    private val contentClient: ContentClient,
    private val configMapper: CoderoomConfigMapper,
    private val publisher: AutogradePublisher,
) {
    @Transactional
    fun createAttempt(
        assignmentId: UUID,
        userId: UUID,
        req: CreateCodeAttemptRequest,
    ): CodeAttemptAggregate {
        val assignment = content.getAssignment(assignmentId, userId)
        val role = access.requireMember(assignment.assignment.courseId, userId)
        if (role != RoleInCourse.STUDENT) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Only students can submit code attempts")
        }
        if (assignment.assignment.assignmentType != AssignmentType.CODE) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Assignment is not a CODE assignment")
        }
        val codeAssignment = codeAssignments.findById(assignmentId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Code assignment not found")
        }
        if (codeAssignment.repositoryPrivate) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Code assignment repository is not published yet")
        }
        val existing = attempts.findAllByAssignmentIdAndStudentUserIdOrderByAttemptNumberAsc(assignmentId, userId)
        if (existing.size >= codeAssignment.maxAttempts) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Maximum number of attempts reached")
        }
        val pullRequestRef = parsePullRequestUrl(req.pullRequestUrl)
        if (pullRequestRef.repositoryFullName != codeAssignment.repositoryFullName) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Pull request must target assignment repository")
        }
        val pullRequest = github.getPullRequest(
            token = null,
            repoFullName = codeAssignment.repositoryFullName,
            pullRequestNumber = pullRequestRef.pullRequestNumber,
        )
        val configFile = github.getFile(
            token = null,
            repoFullName = codeAssignment.repositoryFullName,
            path = ".coderoom.yml",
            ref = codeAssignment.defaultBranch,
        )
        val configSnapshot = configFile.content ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, ".coderoom.yml is empty")
        val parsedConfig = configMapper.parseAndValidate(configSnapshot)
        if (parsedConfig.language != codeAssignment.language) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, ".coderoom.yml language does not match assignment language")
        }
        val now = Instant.now()
        val privateTestsAttachment = codeAssignment.privateTestsAttachmentId?.let { id ->
            attachments.findById(id).orElse(null)
        }
        val attempt = attempts.save(
            CodeSubmissionAttemptEntity(
                attemptId = UUID.randomUUID(),
                courseId = assignment.assignment.courseId,
                assignmentId = assignmentId,
                studentUserId = userId,
                attemptNumber = existing.size + 1,
                pullRequestUrl = pullRequest.htmlUrl,
                pullRequestNumber = pullRequest.number,
                pullRequestHeadSha = pullRequest.headSha,
                repositoryFullName = codeAssignment.repositoryFullName,
                configSnapshot = configSnapshot,
                privateTestsAttachmentId = privateTestsAttachment?.attachmentId,
                status = CodeSubmissionAttemptStatus.QUEUED,
                queuedAt = now,
                createdAt = now,
                updatedAt = now,
            ),
        )
        publishRequested(attempt, codeAssignment, privateTestsAttachment)
        return CodeAttemptAggregate(
            attempt = attempt,
            language = codeAssignment.language,
        )
    }

    @Transactional(readOnly = true)
    fun listMyAttempts(assignmentId: UUID, userId: UUID): List<CodeAttemptAggregate> {
        val assignment = content.getAssignment(assignmentId, userId)
        if (assignment.assignment.assignmentType != AssignmentType.CODE) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Assignment is not a CODE assignment")
        }
        val codeAssignment = codeAssignments.findById(assignmentId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Code assignment not found")
        }
        return attempts.findAllByAssignmentIdAndStudentUserIdOrderByAttemptNumberAsc(assignmentId, userId)
            .map { CodeAttemptAggregate(it, codeAssignment.language) }
    }

    @Transactional(readOnly = true)
    fun listAttemptsForAssignment(assignmentId: UUID, userId: UUID): List<CodeAttemptAggregate> {
        val assignment = content.getAssignment(assignmentId, userId)
        access.requireStaff(assignment.assignment.courseId, userId)
        val codeAssignment = codeAssignments.findById(assignmentId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Code assignment not found")
        }
        return attempts.findAllByAssignmentIdOrderByAttemptNumberAsc(assignmentId)
            .map { CodeAttemptAggregate(it, codeAssignment.language) }
    }

    @Transactional(readOnly = true)
    fun getAttempt(attemptId: UUID, userId: UUID): CodeAttemptAggregate {
        val attempt = attempts.findById(attemptId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Code attempt not found")
        }
        val role = access.requireMember(attempt.courseId, userId)
        if (role == RoleInCourse.STUDENT && attempt.studentUserId != userId) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Code attempt not found")
        }
        val codeAssignment = codeAssignments.findById(attempt.assignmentId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Code assignment not found")
        }
        return CodeAttemptAggregate(attempt, codeAssignment.language)
    }

    @Transactional(readOnly = true)
    fun presignAttemptLogDownload(attemptId: UUID, userId: UUID): AttemptLogDownload {
        val aggregate = getAttempt(attemptId, userId)
        val objectKey = aggregate.attempt.logObjectKey
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Runner log is not available for this attempt")
        val fileName = "code-attempt-${aggregate.attempt.attemptNumber}-runner.log"
        val presigned = contentClient.presignDownload(objectKey = objectKey, fileName = fileName)
        return AttemptLogDownload(
            attemptId = aggregate.attempt.attemptId,
            fileName = fileName,
            presigned = presigned,
        )
    }

    @Transactional
    fun retryAttempt(attemptId: UUID, userId: UUID): CodeAttemptAggregate {
        val attempt = attempts.findById(attemptId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Code attempt not found")
        }
        access.requireStaff(attempt.courseId, userId)
        if (attempt.status == CodeSubmissionAttemptStatus.RUNNING || attempt.status == CodeSubmissionAttemptStatus.QUEUED) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Code attempt is already queued or running")
        }
        val codeAssignment = codeAssignments.findById(attempt.assignmentId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Code assignment not found")
        }
        val privateTestsAttachment = attempt.privateTestsAttachmentId?.let { attachments.findById(it).orElse(null) }
        val now = Instant.now()
        attempt.status = CodeSubmissionAttemptStatus.QUEUED
        attempt.score = null
        attempt.comment = null
        attempt.resultSummary = null
        attempt.logObjectKey = null
        attempt.exitCode = null
        attempt.testsPassed = null
        attempt.testsTotal = null
        attempt.scoringMode = null
        attempt.startedAt = null
        attempt.finishedAt = null
        attempt.queuedAt = now
        attempt.updatedAt = now
        val saved = attempts.save(attempt)
        publishRequested(saved, codeAssignment, privateTestsAttachment)
        return CodeAttemptAggregate(saved, codeAssignment.language)
    }

    @Transactional
    fun applyAutogradeResult(message: AutogradeFinishedMessage) {
        val attempt = attempts.findById(message.attemptId).orElse(null) ?: return
        attempt.status = message.status
        attempt.score = message.score
        attempt.comment = message.comment
        attempt.resultSummary = message.resultSummary
        attempt.logObjectKey = message.logObjectKey
        attempt.exitCode = message.exitCode
        attempt.testsPassed = message.testsPassed
        attempt.testsTotal = message.testsTotal
        attempt.scoringMode = message.scoringMode
        attempt.startedAt = message.startedAt
        attempt.finishedAt = message.finishedAt
        attempt.updatedAt = Instant.now()
        attempts.save(attempt)
    }

    @Transactional(readOnly = true)
    fun latestAttemptsByAssignmentForCourse(courseId: UUID): Map<Pair<UUID, UUID>, CodeSubmissionAttemptEntity> =
        attempts.findAllByCourseId(courseId)
            .groupBy { it.assignmentId to it.studentUserId }
            .mapValues { (_, values) -> values.maxBy { it.attemptNumber } }

    private fun publishRequested(
        attempt: CodeSubmissionAttemptEntity,
        codeAssignment: ru.coderoom.course.domain.CodeAssignmentEntity,
        privateTestsAttachment: AttachmentEntity?,
    ) {
        publisher.publishRequested(
            AutogradeRequestedMessage(
                commandId = UUID.randomUUID(),
                courseId = attempt.courseId,
                assignmentId = attempt.assignmentId,
                attemptId = attempt.attemptId,
                studentUserId = attempt.studentUserId,
                language = codeAssignment.language,
                repositoryFullName = codeAssignment.repositoryFullName,
                repositoryCloneUrl = github.cloneUrl(codeAssignment.repositoryFullName),
                defaultBranch = codeAssignment.defaultBranch,
                pullRequestUrl = attempt.pullRequestUrl,
                pullRequestNumber = attempt.pullRequestNumber,
                pullRequestHeadSha = attempt.pullRequestHeadSha,
                configSnapshot = attempt.configSnapshot,
                privateTests = privateTestsAttachment?.toPayload(),
                queuedAt = attempt.queuedAt,
            ),
        )
    }

    private fun parsePullRequestUrl(url: String): PullRequestRef {
        val match = PULL_REQUEST_URL_REGEX.matchEntire(url.trim())
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid GitHub pull request URL")
        return PullRequestRef(
            repositoryFullName = "${match.groupValues[1]}/${match.groupValues[2]}",
            pullRequestNumber = match.groupValues[3].toInt(),
        )
    }

    private fun AttachmentEntity.toPayload(): AttachedObjectPayload =
        AttachedObjectPayload(
            bucket = storageBucket,
            objectKey = objectKey,
            fileName = originalFilename,
        )

    private data class PullRequestRef(
        val repositoryFullName: String,
        val pullRequestNumber: Int,
    )

    companion object {
        private val PULL_REQUEST_URL_REGEX =
            Regex("""https://github\.com/([^/\s]+)/([^/\s]+)/pull/(\d+)/?""", RegexOption.IGNORE_CASE)
    }
}

data class CodeAttemptAggregate(
    val attempt: CodeSubmissionAttemptEntity,
    val language: ru.coderoom.course.domain.CodeLanguage,
)

data class AttemptLogDownload(
    val attemptId: UUID,
    val fileName: String,
    val presigned: PresignedUrlResponse,
)
