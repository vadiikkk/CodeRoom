package ru.coderoom.course.api

import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.coderoom.course.api.dto.CourseResponse
import ru.coderoom.course.api.dto.CreateCourseRequest
import ru.coderoom.course.api.dto.MyMembershipResponse
import ru.coderoom.course.api.dto.UpdateCourseRequest
import ru.coderoom.course.security.RequestUser
import ru.coderoom.course.service.CourseAccessService
import ru.coderoom.course.service.CourseAppService
import java.util.UUID

@RestController
@RequestMapping("/api/v1/courses")
class CoursesController(
    private val app: CourseAppService,
    private val access: CourseAccessService,
) {
    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    fun create(
        @AuthenticationPrincipal user: RequestUser,
        @Valid @RequestBody req: CreateCourseRequest,
    ): CourseResponse {
        val course = app.createCourse(
            ownerUserId = user.userId,
            title = req.title.trim(),
            description = req.description?.trim(),
            isVisible = req.isVisible,
        )
        return toResponse(courseId = course.courseId, user = user)
    }

    @GetMapping
    fun listMine(
        @AuthenticationPrincipal user: RequestUser,
    ): List<CourseResponse> =
        app.listMyCourses(user.userId).map { toResponse(courseId = it.courseId, user = user) }

    @GetMapping("/{courseId}")
    fun get(
        @AuthenticationPrincipal user: RequestUser,
        @PathVariable courseId: UUID,
    ): CourseResponse =
        toResponse(courseId = courseId, user = user)

    @PutMapping("/{courseId}")
    fun update(
        @AuthenticationPrincipal user: RequestUser,
        @PathVariable courseId: UUID,
        @Valid @RequestBody req: UpdateCourseRequest,
    ): CourseResponse {
        app.updateCourse(
            courseId = courseId,
            userId = user.userId,
            title = req.title?.trim(),
            description = req.description?.trim(),
            isVisible = req.isVisible,
        )
        return toResponse(courseId = courseId, user = user)
    }

    @DeleteMapping("/{courseId}")
    fun delete(
        @AuthenticationPrincipal user: RequestUser,
        @PathVariable courseId: UUID,
    ) {
        app.deleteCourse(courseId = courseId, userId = user.userId)
    }

    @GetMapping("/{courseId}/membership/me")
    fun myMembership(
        @AuthenticationPrincipal user: RequestUser,
        @PathVariable courseId: UUID,
    ): MyMembershipResponse {
        val role = access.requireMember(courseId, user.userId)
        return MyMembershipResponse(courseId = courseId, userId = user.userId, roleInCourse = role)
    }

    private fun toResponse(courseId: UUID, user: RequestUser): CourseResponse {
        val course = app.getCourseForMember(courseId, user.userId)
        val role = access.requireMember(courseId, user.userId)
        return CourseResponse(
            courseId = course.courseId,
            ownerUserId = course.ownerUserId,
            title = course.title,
            description = course.description,
            isVisible = course.isVisible,
            githubPatConfigured = app.githubPatConfigured(course.courseId),
            myRoleInCourse = role.name,
            createdAt = course.createdAt,
            updatedAt = course.updatedAt,
        )
    }
}
