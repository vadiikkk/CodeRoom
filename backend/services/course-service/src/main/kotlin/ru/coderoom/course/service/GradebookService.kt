package ru.coderoom.course.service

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import ru.coderoom.course.api.dto.CourseGradebookResponse
import ru.coderoom.course.api.dto.GradebookAssignmentResponse
import ru.coderoom.course.api.dto.GradebookEntryResponse
import ru.coderoom.course.api.dto.GradebookStudentRowResponse
import ru.coderoom.course.api.dto.MyGradebookResponse
import ru.coderoom.course.domain.AssignmentType
import ru.coderoom.course.domain.CodeSubmissionAttemptEntity
import ru.coderoom.course.domain.RoleInCourse
import ru.coderoom.course.domain.SubmissionEntity
import ru.coderoom.course.identity.IdentityClient
import ru.coderoom.course.repo.CourseEnrollmentRepository
import ru.coderoom.course.repo.SubmissionMemberRepository
import ru.coderoom.course.repo.SubmissionRepository
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

@Service
class GradebookService(
    private val access: CourseAccessService,
    private val content: CourseContentService,
    private val enrollments: CourseEnrollmentRepository,
    private val submissions: SubmissionRepository,
    private val submissionMembers: SubmissionMemberRepository,
    private val groups: CourseGroupService,
    private val identity: IdentityClient,
    private val codeAttempts: CodeAttemptService,
) {
    @Transactional(readOnly = true)
    fun courseGradebook(courseId: UUID, userId: UUID): CourseGradebookResponse {
        access.requireStaff(courseId, userId)
        val assignmentAggregates = content.listAssignments(courseId, userId)
        val assignmentColumns = assignmentAggregates.map {
            GradebookAssignmentResponse(
                assignmentId = it.assignment.assignmentId,
                title = it.assignment.title,
                assignmentType = it.assignment.assignmentType,
                weight = it.assignment.weight,
            )
        }

        val students = enrollments.findAllByCourseId(courseId)
            .filter { it.roleInCourse == RoleInCourse.STUDENT }
            .sortedBy { it.createdAt }
        val submissionsById = submissions.findAllByCourseIdOrderBySubmittedAtAsc(courseId).associateBy { it.submissionId }
        val submissionMembersBySubmissionId = submissionMembers.findAll()
            .filter { submissionsById.containsKey(it.submissionId) }
            .groupBy({ it.submissionId }, { it.userId })
        val groupNamesById = submissionsById.values.mapNotNull { it.ownerGroupId }.distinct().associateWith { groupId ->
            groups.requireGroup(courseId, groupId).name
        }
        val latestCodeAttempts = codeAttempts.latestAttemptsByAssignmentForCourse(courseId)

        val rows = students.map { student ->
            val entries = assignmentAggregates.map { assignment ->
                when (assignment.assignment.assignmentType) {
                    AssignmentType.CODE ->
                        toEntry(
                            assignmentId = assignment.assignment.assignmentId,
                            weight = assignment.assignment.weight,
                            codeAttempt = latestCodeAttempts[assignment.assignment.assignmentId to student.userId],
                        )

                    else -> {
                        val submission = submissionsById.values.firstOrNull { candidate ->
                            candidate.assignmentId == assignment.assignment.assignmentId &&
                                submissionMembersBySubmissionId[candidate.submissionId].orEmpty().contains(student.userId)
                        }
                        toEntry(assignment.assignment.assignmentId, assignment.assignment.weight, submission, groupNamesById)
                    }
                }
            }
            GradebookStudentRowResponse(
                userId = student.userId,
                entries = entries,
                totalWeightedScore = entries.fold(BigDecimal.ZERO) { acc, entry -> acc + (entry.weightedScore ?: BigDecimal.ZERO) }
                    .setScale(2, RoundingMode.HALF_UP),
            )
        }

        return CourseGradebookResponse(assignments = assignmentColumns, rows = rows)
    }

    @Transactional(readOnly = true)
    fun myGradebook(courseId: UUID, userId: UUID): MyGradebookResponse {
        val role = access.requireMember(courseId, userId)
        if (role != RoleInCourse.STUDENT) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Only students can view personal gradebook")
        }

        val assignmentAggregates = content.listAssignments(courseId, userId)
        val submissionsForUser = submissionMembers.findAllByUserId(userId)
            .mapNotNull { member -> submissions.findById(member.submissionId).orElse(null) }
            .filter { it.courseId == courseId }
            .associateBy { it.assignmentId }
        val codeAttemptsForUser = codeAttempts.latestAttemptsByAssignmentForCourse(courseId)

        val entries = assignmentAggregates.map { assignment ->
            when (assignment.assignment.assignmentType) {
                AssignmentType.CODE ->
                    toEntry(
                        assignmentId = assignment.assignment.assignmentId,
                        weight = assignment.assignment.weight,
                        codeAttempt = codeAttemptsForUser[assignment.assignment.assignmentId to userId],
                    )

                else ->
                    toEntry(
                        assignmentId = assignment.assignment.assignmentId,
                        weight = assignment.assignment.weight,
                        submission = submissionsForUser[assignment.assignment.assignmentId],
                        groupNamesById = emptyMap(),
                    )
            }
        }

        return MyGradebookResponse(
            userId = userId,
            assignments = assignmentAggregates.map {
                GradebookAssignmentResponse(
                    assignmentId = it.assignment.assignmentId,
                    title = it.assignment.title,
                    assignmentType = it.assignment.assignmentType,
                    weight = it.assignment.weight,
                )
            },
            entries = entries,
            totalWeightedScore = entries.fold(BigDecimal.ZERO) { acc, entry -> acc + (entry.weightedScore ?: BigDecimal.ZERO) }
                .setScale(2, RoundingMode.HALF_UP),
        )
    }

    @Transactional(readOnly = true)
    fun courseGradebookCsv(courseId: UUID, userId: UUID, authHeader: String): String {
        val gradebook = courseGradebook(courseId, userId)
        val emailsByUserId = identity.lookupUsersByIds(
            authHeader = authHeader,
            userIds = gradebook.rows.map { it.userId },
        ).associate { it.userId to it.email }

        val header = buildList {
            add("email")
            repeat(gradebook.assignments.size) { index ->
                add("task${index + 1}grade")
            }
            add("currentFinalGrade")
        }

        val lines = mutableListOf(header.joinToString(";"))
        gradebook.rows.forEach { row ->
            val values = mutableListOf<String>()
            values += csvCell(emailsByUserId[row.userId] ?: row.userId.toString())
            row.entries.forEach { entry ->
                values += csvCell(entry.score?.stripTrailingZeros()?.toPlainString().orEmpty())
            }
            values += csvCell(row.totalWeightedScore.stripTrailingZeros().toPlainString())
            lines += values.joinToString(";")
        }
        return lines.joinToString(System.lineSeparator())
    }

    private fun toEntry(
        assignmentId: UUID,
        weight: BigDecimal,
        submission: SubmissionEntity?,
        groupNamesById: Map<UUID, String>,
    ): GradebookEntryResponse {
        val weightedScore = submission?.score?.multiply(weight)?.setScale(2, RoundingMode.HALF_UP)
        return GradebookEntryResponse(
            assignmentId = assignmentId,
            submissionId = submission?.submissionId,
            codeAttemptId = null,
            status = submission?.status?.name,
            score = submission?.score,
            weightedScore = weightedScore,
            comment = submission?.comment,
            ownerType = submission?.ownerType,
            ownerGroupId = submission?.ownerGroupId,
            ownerGroupName = submission?.ownerGroupId?.let { groupNamesById[it] ?: groups.requireGroup(submission.courseId, it).name },
            submittedAt = submission?.submittedAt,
            gradedAt = submission?.gradedAt,
        )
    }

    private fun toEntry(
        assignmentId: UUID,
        weight: BigDecimal,
        codeAttempt: CodeSubmissionAttemptEntity?,
    ): GradebookEntryResponse {
        val weightedScore = codeAttempt?.score?.multiply(weight)?.setScale(2, RoundingMode.HALF_UP)
        return GradebookEntryResponse(
            assignmentId = assignmentId,
            submissionId = null,
            codeAttemptId = codeAttempt?.attemptId,
            status = codeAttempt?.status?.name,
            score = codeAttempt?.score,
            weightedScore = weightedScore,
            comment = codeAttempt?.comment,
            ownerType = null,
            ownerGroupId = null,
            ownerGroupName = null,
            submittedAt = codeAttempt?.queuedAt,
            gradedAt = codeAttempt?.finishedAt,
        )
    }

    private fun csvCell(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }
}
