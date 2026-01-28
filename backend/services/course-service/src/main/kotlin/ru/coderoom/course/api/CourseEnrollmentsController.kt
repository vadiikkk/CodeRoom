package ru.coderoom.course.api

import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.coderoom.course.api.dto.EnrollmentResponse
import ru.coderoom.course.api.dto.UpsertEnrollmentRequest
import ru.coderoom.course.security.RequestUser
import ru.coderoom.course.service.CourseAppService
import java.util.UUID

@RestController
@RequestMapping("/api/v1/courses/{courseId}/enrollments")
class CourseEnrollmentsController(
    private val app: CourseAppService,
) {
    @GetMapping
    fun list(
        @AuthenticationPrincipal user: RequestUser,
        @PathVariable courseId: UUID,
    ): List<EnrollmentResponse> =
        app.listEnrollments(courseId, user.userId).map {
            EnrollmentResponse(
                userId = it.userId,
                roleInCourse = it.roleInCourse,
                createdAt = it.createdAt,
            )
        }

    @PostMapping
    fun upsert(
        @AuthenticationPrincipal user: RequestUser,
        @PathVariable courseId: UUID,
        @Valid @RequestBody req: UpsertEnrollmentRequest,
    ) {
        app.upsertEnrollment(
            courseId = courseId,
            requesterUserId = user.userId,
            targetUserId = req.userId,
            roleInCourse = req.roleInCourse,
        )
    }

    @DeleteMapping("/{userId}")
    fun remove(
        @AuthenticationPrincipal user: RequestUser,
        @PathVariable courseId: UUID,
        @PathVariable userId: UUID,
    ) {
        app.removeEnrollment(courseId = courseId, requesterUserId = user.userId, targetUserId = userId)
    }
}
