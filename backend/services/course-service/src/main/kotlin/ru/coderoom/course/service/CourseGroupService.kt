package ru.coderoom.course.service

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import ru.coderoom.course.domain.CourseGroupEntity
import ru.coderoom.course.domain.CourseGroupMemberEntity
import ru.coderoom.course.domain.RoleInCourse
import ru.coderoom.course.repo.CourseEnrollmentRepository
import ru.coderoom.course.repo.CourseGroupMemberRepository
import ru.coderoom.course.repo.CourseGroupRepository
import ru.coderoom.course.repo.SubmissionRepository
import java.time.Instant
import java.util.UUID

@Service
class CourseGroupService(
    private val access: CourseAccessService,
    private val enrollments: CourseEnrollmentRepository,
    private val groups: CourseGroupRepository,
    private val members: CourseGroupMemberRepository,
    private val submissions: SubmissionRepository,
) {
    @Transactional(readOnly = true)
    fun listGroups(courseId: UUID, userId: UUID): List<CourseGroupAggregate> {
        access.requireMember(courseId, userId)
        val courseGroups = groups.findAllByCourseIdOrderByCreatedAtAsc(courseId)
        val membersByGroup = members.findAllByCourseIdOrderByCreatedAtAsc(courseId).groupBy { it.groupId }
        return courseGroups.map { group ->
            CourseGroupAggregate(group = group, members = membersByGroup[group.groupId].orEmpty())
        }
    }

    @Transactional
    fun createGroup(courseId: UUID, userId: UUID, name: String): CourseGroupAggregate {
        access.requireTeacher(courseId, userId)
        access.requireCourse(courseId)
        validateGroupName(courseId, name)

        val now = Instant.now()
        val group = groups.save(
            CourseGroupEntity(
                groupId = UUID.randomUUID(),
                courseId = courseId,
                name = name,
                createdAt = now,
                updatedAt = now,
            ),
        )
        return CourseGroupAggregate(group = group, members = emptyList())
    }

    @Transactional
    fun updateGroup(courseId: UUID, userId: UUID, groupId: UUID, name: String): CourseGroupAggregate {
        access.requireTeacher(courseId, userId)
        val group = requireGroup(courseId, groupId)
        if (!group.name.equals(name, ignoreCase = true)) {
            validateGroupName(courseId, name)
        }
        group.name = name
        group.updatedAt = Instant.now()
        val saved = groups.save(group)
        return CourseGroupAggregate(saved, members.findAllByGroupIdOrderByCreatedAtAsc(groupId))
    }

    @Transactional
    fun deleteGroup(courseId: UUID, userId: UUID, groupId: UUID) {
        access.requireTeacher(courseId, userId)
        requireGroup(courseId, groupId)
        if (submissions.existsByOwnerGroupId(groupId)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot delete group with existing submissions")
        }
        groups.deleteById(groupId)
    }

    @Transactional
    fun addMember(courseId: UUID, userId: UUID, groupId: UUID, memberUserId: UUID): CourseGroupAggregate {
        access.requireTeacher(courseId, userId)
        requireGroup(courseId, groupId)
        val enrollment = enrollments.findByCourseIdAndUserId(courseId, memberUserId)
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not enrolled in course")
        if (enrollment.roleInCourse != RoleInCourse.STUDENT) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Only students can be added to groups")
        }

        val existingMembership = members.findByCourseIdAndUserId(courseId, memberUserId)
        if (existingMembership != null && existingMembership.groupId != groupId) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Student already belongs to another group")
        }
        if (existingMembership == null) {
            members.save(
                CourseGroupMemberEntity(
                    groupId = groupId,
                    courseId = courseId,
                    userId = memberUserId,
                    createdAt = Instant.now(),
                ),
            )
        }
        return CourseGroupAggregate(requireGroup(courseId, groupId), members.findAllByGroupIdOrderByCreatedAtAsc(groupId))
    }

    @Transactional
    fun removeMember(courseId: UUID, userId: UUID, groupId: UUID, memberUserId: UUID): CourseGroupAggregate {
        access.requireTeacher(courseId, userId)
        requireGroup(courseId, groupId)
        val existing = members.findAllByGroupIdOrderByCreatedAtAsc(groupId).firstOrNull { it.userId == memberUserId }
        if (existing != null) {
            members.delete(existing)
        }
        return CourseGroupAggregate(requireGroup(courseId, groupId), members.findAllByGroupIdOrderByCreatedAtAsc(groupId))
    }

    @Transactional(readOnly = true)
    fun findGroupForStudent(courseId: UUID, studentUserId: UUID): CourseGroupAggregate? {
        val membership = members.findByCourseIdAndUserId(courseId, studentUserId) ?: return null
        return CourseGroupAggregate(
            group = requireGroup(courseId, membership.groupId),
            members = members.findAllByGroupIdOrderByCreatedAtAsc(membership.groupId),
        )
    }

    @Transactional(readOnly = true)
    fun requireGroup(courseId: UUID, groupId: UUID): CourseGroupEntity =
        groups.findById(groupId).orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found") }
            .also {
                if (it.courseId != courseId) {
                    throw ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found")
                }
            }

    private fun validateGroupName(courseId: UUID, name: String) {
        if (groups.existsByCourseIdAndNameIgnoreCase(courseId, name)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Group with this name already exists")
        }
    }
}

data class CourseGroupAggregate(
    val group: CourseGroupEntity,
    val members: List<CourseGroupMemberEntity>,
)
