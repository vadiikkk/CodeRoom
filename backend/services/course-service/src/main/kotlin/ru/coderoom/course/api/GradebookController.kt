package ru.coderoom.course.api

import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import ru.coderoom.course.api.dto.CourseGradebookResponse
import ru.coderoom.course.api.dto.MyGradebookResponse
import ru.coderoom.course.security.RequestUser
import ru.coderoom.course.service.GradebookService
import java.util.UUID

@RestController
@RequestMapping("/api/v1/courses/{courseId}/gradebook")
class GradebookController(
    private val gradebook: GradebookService,
) {
    @GetMapping
    fun courseGradebook(
        @AuthenticationPrincipal user: RequestUser,
        @PathVariable courseId: UUID,
    ): CourseGradebookResponse =
        gradebook.courseGradebook(courseId, user.userId)

    @GetMapping("/me")
    fun myGradebook(
        @AuthenticationPrincipal user: RequestUser,
        @PathVariable courseId: UUID,
    ): MyGradebookResponse =
        gradebook.myGradebook(courseId, user.userId)

    @GetMapping("/csv")
    fun courseGradebookCsv(
        @AuthenticationPrincipal user: RequestUser,
        @PathVariable courseId: UUID,
        @RequestHeader("Authorization") authorization: String,
    ): ResponseEntity<String> {
        val csv = gradebook.courseGradebookCsv(courseId, user.userId, authorization)
        return ResponseEntity.ok()
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment()
                    .filename("course-$courseId-gradebook.csv")
                    .build()
                    .toString(),
            )
            .contentType(MediaType.parseMediaType("text/csv"))
            .body(csv)
    }
}
