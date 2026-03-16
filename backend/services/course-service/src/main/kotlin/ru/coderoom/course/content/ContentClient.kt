package ru.coderoom.course.content

import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.server.ResponseStatusException

data class PresignUploadObjectRequest(
    val objectKey: String,
    val contentType: String?,
)

data class PresignDownloadObjectRequest(
    val objectKey: String,
    val fileName: String,
)

data class DeleteObjectRequest(
    val objectKey: String,
)

data class PresignedUrlResponse(
    val url: String,
    val method: String,
)

@Component
class ContentClient(
    props: ContentProperties,
) {
    private val client: RestClient =
        RestClient.builder()
            .baseUrl(props.baseUrl.trimEnd('/'))
            .build()

    fun presignUpload(objectKey: String, contentType: String?): PresignedUrlResponse =
        execute("Failed to presign upload in content-service") {
            client.post()
                .uri("/internal/api/v1/objects/presign-upload")
                .contentType(MediaType.APPLICATION_JSON)
                .body(PresignUploadObjectRequest(objectKey = objectKey, contentType = contentType))
                .retrieve()
                .body(PresignedUrlResponse::class.java)
        }

    fun presignDownload(objectKey: String, fileName: String): PresignedUrlResponse =
        execute("Failed to presign download in content-service") {
            client.post()
                .uri("/internal/api/v1/objects/presign-download")
                .contentType(MediaType.APPLICATION_JSON)
                .body(PresignDownloadObjectRequest(objectKey = objectKey, fileName = fileName))
                .retrieve()
                .body(PresignedUrlResponse::class.java)
        }

    fun deleteObject(objectKey: String) {
        execute("Failed to delete object in content-service") {
            client.method(org.springframework.http.HttpMethod.DELETE)
                .uri("/internal/api/v1/objects")
                .contentType(MediaType.APPLICATION_JSON)
                .body(DeleteObjectRequest(objectKey = objectKey))
                .retrieve()
                .toBodilessEntity()
            Unit
        }
    }

    private fun <T> execute(message: String, action: () -> T?): T =
        action() ?: throw ResponseStatusException(HttpStatus.BAD_GATEWAY, message)
}
