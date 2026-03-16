package ru.coderoom.course.repo

import org.springframework.data.jpa.repository.JpaRepository
import ru.coderoom.course.domain.CourseGroupMemberEntity
import ru.coderoom.course.domain.CourseGroupMemberId
import java.util.UUID

interface CourseGroupMemberRepository : JpaRepository<CourseGroupMemberEntity, CourseGroupMemberId> {
    fun findAllByCourseIdOrderByCreatedAtAsc(courseId: UUID): List<CourseGroupMemberEntity>
    fun findAllByGroupIdOrderByCreatedAtAsc(groupId: UUID): List<CourseGroupMemberEntity>
    fun findAllByUserId(userId: UUID): List<CourseGroupMemberEntity>
    fun findByCourseIdAndUserId(courseId: UUID, userId: UUID): CourseGroupMemberEntity?
    fun existsByCourseIdAndUserId(courseId: UUID, userId: UUID): Boolean
    fun existsByGroupIdAndUserId(groupId: UUID, userId: UUID): Boolean
}
