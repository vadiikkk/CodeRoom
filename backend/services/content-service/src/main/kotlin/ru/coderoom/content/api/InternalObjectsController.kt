package ru.coderoom.content.api

import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.coderoom.content.api.dto.DeleteObjectRequest
import ru.coderoom.content.api.dto.DownloadObjectRequest
import ru.coderoom.content.api.dto.PresignDownloadObjectRequest
import ru.coderoom.content.api.dto.PresignUploadObjectRequest
import ru.coderoom.content.api.dto.PresignedUrlResponse
import ru.coderoom.content.storage.StorageService

@RestController
@RequestMapping("/internal/api/v1/objects")
class InternalObjectsController(
    private val storage: StorageService,
) {
    @PostMapping("/presign-upload")
    fun presignUpload(
        @Valid @RequestBody req: PresignUploadObjectRequest,
    ): PresignedUrlResponse =
        PresignedUrlResponse(
            url = storage.presignUpload(objectKey = req.objectKey.trim()),
            method = "PUT",
        )

    @PostMapping("/presign-download")
    fun presignDownload(
        @Valid @RequestBody req: PresignDownloadObjectRequest,
    ): PresignedUrlResponse =
        PresignedUrlResponse(
            url = storage.presignDownload(
                objectKey = req.objectKey.trim(),
                fileName = req.fileName.trim(),
            ),
            method = "GET",
        )

    @PostMapping("/download")
    fun downloadObject(
        @Valid @RequestBody req: DownloadObjectRequest,
    ): ResponseEntity<ByteArray> =
        ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(storage.downloadObject(req.objectKey.trim()))

    @PostMapping("/upload")
    fun uploadObject(
        @RequestParam objectKey: String,
        @RequestParam(required = false) contentType: String?,
        @RequestBody body: ByteArray,
    ) {
        storage.uploadObject(
            objectKey = objectKey.trim(),
            contentType = contentType?.trim()?.ifBlank { null },
            bytes = body,
        )
    }

    @DeleteMapping
    fun deleteObject(
        @Valid @RequestBody req: DeleteObjectRequest,
    ) {
        storage.deleteObject(req.objectKey.trim())
    }
}
