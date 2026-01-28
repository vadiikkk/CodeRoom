package ru.coderoom.course.service

import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import org.springframework.http.HttpStatus
import ru.coderoom.course.domain.CourseEntity
import ru.coderoom.course.domain.RoleInCourse
import ru.coderoom.course.repo.CourseEnrollmentRepository
import ru.coderoom.course.repo.CourseRepository
import java.util.UUID

@Service
class CourseAccessService(
    private val courses: CourseRepository,
    private val enrollments: CourseEnrollmentRepository,
) {
    fun requireCourse(courseId: UUID): CourseEntity =
        courses.findById(courseId).orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found") }

    fun roleInCourseOrNull(courseId: UUID, userId: UUID): RoleInCourse? =
        enrollments.findByCourseIdAndUserId(courseId, userId)?.roleInCourse

    fun requireMember(courseId: UUID, userId: UUID): RoleInCourse {
        requireCourse(courseId)
        return roleInCourseOrNull(courseId, userId)
            ?: throw ResponseStatusException(HttpStatus.FORBIDDEN, "Not a course member")
    }

    fun requireTeacher(courseId: UUID, userId: UUID) {
        val role = requireMember(courseId, userId)
        if (role != RoleInCourse.TEACHER) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Teacher role required in course")
        }
    }
}
