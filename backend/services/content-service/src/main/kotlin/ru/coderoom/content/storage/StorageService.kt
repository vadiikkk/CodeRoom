package ru.coderoom.content.storage

import io.minio.BucketExistsArgs
import io.minio.GetObjectArgs
import io.minio.GetPresignedObjectUrlArgs
import io.minio.MakeBucketArgs
import io.minio.MinioClient
import io.minio.PutObjectArgs
import io.minio.RemoveObjectArgs
import io.minio.http.Method
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

@Service
class StorageService(
    private val props: StorageProperties,
) {
    private val internalClient: MinioClient =
        MinioClient.builder()
            .endpoint(props.internalEndpoint)
            .credentials(props.accessKey, props.secretKey)
            .region(props.region)
            .build()

    private val presignClient: MinioClient =
        MinioClient.builder()
            .endpoint(props.publicEndpoint)
            .credentials(props.accessKey, props.secretKey)
            .region(props.region)
            .build()

    @PostConstruct
    fun ensureBucket() {
        val exists = internalClient.bucketExists(BucketExistsArgs.builder().bucket(props.bucket).build())
        if (!exists) {
            internalClient.makeBucket(MakeBucketArgs.builder().bucket(props.bucket).build())
        }
    }

    fun presignUpload(objectKey: String): String =
        presignClient.getPresignedObjectUrl(
            GetPresignedObjectUrlArgs.builder()
                .method(Method.PUT)
                .bucket(props.bucket)
                .`object`(objectKey)
                .expiry(props.uploadExpiryMinutes, TimeUnit.MINUTES)
                .build(),
        )

    fun presignDownload(objectKey: String, fileName: String): String =
        presignClient.getPresignedObjectUrl(
            GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(props.bucket)
                .`object`(objectKey)
                .extraQueryParams(
                    mapOf(
                        "response-content-disposition" to
                            "attachment; filename*=UTF-8''${encodeFileName(fileName)}",
                    ),
                )
                .expiry(props.downloadExpiryMinutes, TimeUnit.MINUTES)
                .build(),
        )

    fun downloadObject(objectKey: String): ByteArray =
        internalClient.getObject(
            GetObjectArgs.builder()
                .bucket(props.bucket)
                .`object`(objectKey)
                .build(),
        ).use { it.readAllBytes() }

    fun uploadObject(objectKey: String, contentType: String?, bytes: ByteArray) {
        internalClient.putObject(
            PutObjectArgs.builder()
                .bucket(props.bucket)
                .`object`(objectKey)
                .contentType(contentType ?: "application/octet-stream")
                .stream(ByteArrayInputStream(bytes), bytes.size.toLong(), -1)
                .build(),
        )
    }

    fun deleteObject(objectKey: String) {
        internalClient.removeObject(
            RemoveObjectArgs.builder()
                .bucket(props.bucket)
                .`object`(objectKey)
                .build(),
        )
    }

    private fun encodeFileName(fileName: String): String =
        URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20")
}
