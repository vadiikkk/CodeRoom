package ru.coderoom.course.identity

import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.server.ResponseStatusException
import org.springframework.http.HttpStatus
import java.util.UUID

data class LookupUsersByEmailRequest(
    val emails: List<String>,
)

data class LookupUsersByIdRequest(
    val userIds: List<UUID>,
)

data class LookupUserDto(
    val userId: UUID,
    val email: String,
)

data class LookupUsersByEmailResponse(
    val users: List<LookupUserDto>,
)

@Component
class IdentityClient(
    props: IdentityProperties,
) {
    private val client: RestClient =
        RestClient.builder()
            .baseUrl(props.baseUrl.trimEnd('/'))
            .build()

    fun lookupUsersByEmail(authHeader: String, emails: List<String>): List<LookupUserDto> {
        val safe = emails.map { it.trim().lowercase() }.filter { it.isNotBlank() }.distinct()
        if (safe.isEmpty()) return emptyList()

        val resp = runCatching {
            client.post()
                .uri("/api/v1/users/lookup")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .body(LookupUsersByEmailRequest(emails = safe))
                .retrieve()
                .body(LookupUsersByEmailResponse::class.java)
        }.getOrNull()

        if (resp == null) {
            throw ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to resolve users in identity-service")
        }
        return resp.users
    }

    fun lookupUsersByIds(authHeader: String, userIds: List<UUID>): List<LookupUserDto> {
        val safe = userIds.distinct()
        if (safe.isEmpty()) return emptyList()

        val resp = runCatching {
            client.post()
                .uri("/api/v1/users/lookup-by-ids")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .body(LookupUsersByIdRequest(userIds = safe))
                .retrieve()
                .body(LookupUsersByEmailResponse::class.java)
        }.getOrNull()

        if (resp == null) {
            throw ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to resolve users in identity-service")
        }
        return resp.users
    }
}

