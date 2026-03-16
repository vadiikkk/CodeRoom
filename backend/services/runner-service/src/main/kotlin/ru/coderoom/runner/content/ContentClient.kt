package ru.coderoom.runner.content

import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.server.ResponseStatusException
import ru.coderoom.runner.config.ContentProperties

private data class DownloadObjectRequest(
    val objectKey: String,
)

@Component
class ContentClient(
    props: ContentProperties,
) {
    private val client: RestClient =
        RestClient.builder()
            .baseUrl(props.baseUrl.trimEnd('/'))
            .build()

    fun downloadBytes(objectKey: String, @Suppress("UNUSED_PARAMETER") fileName: String): ByteArray {
        return client.post()
            .uri("/internal/api/v1/objects/download")
            .contentType(MediaType.APPLICATION_JSON)
            .body(DownloadObjectRequest(objectKey = objectKey))
            .retrieve()
            .body(ByteArray::class.java)
            ?: throw ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to download private tests artifact")
    }

    fun uploadBytes(objectKey: String, contentType: String, bytes: ByteArray): String {
        client.post()
            .uri { builder ->
                builder
                    .path("/internal/api/v1/objects/upload")
                    .queryParam("objectKey", objectKey)
                    .queryParam("contentType", contentType)
                    .build()
            }
            .contentType(MediaType.parseMediaType(contentType))
            .body(bytes)
            .retrieve()
            .toBodilessEntity()
        return objectKey
    }
}
