package ru.coderoom.course.api

import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.coderoom.course.api.dto.PresignDownloadAttachmentRequest
import ru.coderoom.course.api.dto.PresignDownloadAttachmentResponse
import ru.coderoom.course.api.dto.PresignUploadAttachmentRequest
import ru.coderoom.course.api.dto.PresignUploadAttachmentResponse
import ru.coderoom.course.security.RequestUser
import ru.coderoom.course.service.CourseContentService

@RestController
@RequestMapping("/api/v1/attachments")
class AttachmentsController(
    private val content: CourseContentService,
) {
    @PostMapping("/presign-upload")
    fun presignUpload(
        @AuthenticationPrincipal user: RequestUser,
        @Valid @RequestBody req: PresignUploadAttachmentRequest,
    ): PresignUploadAttachmentResponse {
        val upload = content.createUploadAttachment(
            courseId = req.courseId,
            userId = user.userId,
            fileName = req.fileName.trim(),
            contentType = req.contentType?.trim()?.ifBlank { null },
            fileSize = req.fileSize,
        )
        return PresignUploadAttachmentResponse(
            attachmentId = upload.attachment.attachmentId,
            courseId = upload.attachment.courseId,
            fileName = upload.attachment.originalFilename,
            contentType = upload.attachment.contentType,
            fileSize = upload.attachment.fileSize,
            uploadUrl = upload.uploadUrl,
            method = upload.method,
            createdAt = upload.attachment.createdAt,
        )
    }

    @PostMapping("/presign-download")
    fun presignDownload(
        @AuthenticationPrincipal user: RequestUser,
        @Valid @RequestBody req: PresignDownloadAttachmentRequest,
    ): PresignDownloadAttachmentResponse {
        val download = content.presignDownloadAttachment(
            attachmentId = req.attachmentId,
            userId = user.userId,
        )
        return PresignDownloadAttachmentResponse(
            attachmentId = download.attachment.attachmentId,
            fileName = download.attachment.originalFilename,
            contentType = download.attachment.contentType,
            fileSize = download.attachment.fileSize,
            downloadUrl = download.downloadUrl,
            method = download.method,
        )
    }
}
