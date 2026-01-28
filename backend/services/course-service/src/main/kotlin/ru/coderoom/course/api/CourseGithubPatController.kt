package ru.coderoom.course.api

import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.coderoom.course.api.dto.GithubPatStatusResponse
import ru.coderoom.course.api.dto.SetGithubPatRequest
import ru.coderoom.course.security.RequestUser
import ru.coderoom.course.service.CourseAppService
import java.util.UUID

@RestController
@RequestMapping("/api/v1/courses/{courseId}/github-pat")
class CourseGithubPatController(
    private val app: CourseAppService,
) {
    @GetMapping
    fun status(
        @AuthenticationPrincipal user: RequestUser,
        @PathVariable courseId: UUID,
    ): GithubPatStatusResponse {
        val s = app.githubPatStatus(courseId = courseId, userId = user.userId)
        return GithubPatStatusResponse(configured = s.configured, updatedAt = s.updatedAt)
    }

    @PutMapping
    fun set(
        @AuthenticationPrincipal user: RequestUser,
        @PathVariable courseId: UUID,
        @Valid @RequestBody req: SetGithubPatRequest,
    ) {
        app.setGithubPat(courseId = courseId, userId = user.userId, token = req.token)
    }

    @DeleteMapping
    fun clear(
        @AuthenticationPrincipal user: RequestUser,
        @PathVariable courseId: UUID,
    ) {
        app.clearGithubPat(courseId = courseId, userId = user.userId)
    }
}
