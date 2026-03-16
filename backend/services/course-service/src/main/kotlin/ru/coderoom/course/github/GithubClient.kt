package ru.coderoom.course.github

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.server.ResponseStatusException
import java.nio.charset.StandardCharsets
import java.util.Base64

data class GithubRepositoryResponse(
    val name: String,
    val fullName: String,
    val htmlUrl: String,
    val defaultBranch: String,
    val private: Boolean,
)

data class GithubContentFileResponse(
    val sha: String,
    val content: String?,
)

data class GithubPullRequestResponse(
    val number: Int,
    val htmlUrl: String,
    val state: String,
    val headSha: String,
)

@Component
class GithubClient(
    props: GithubProperties,
) {
    private val apiBaseUrl = props.apiBaseUrl.trimEnd('/')
    private val cloneBaseUrl = props.cloneBaseUrl.trimEnd('/')
    private val client: RestClient =
        RestClient.builder()
            .baseUrl(apiBaseUrl)
            .build()

    fun createPrivateRepository(token: String, name: String, description: String?): GithubRepositoryResponse =
        execute("Failed to create GitHub repository") {
            client.post()
                .uri("/user/repos")
                .headers { headers(token, it) }
                .contentType(MediaType.APPLICATION_JSON)
                .body(
                    mapOf(
                        "name" to name,
                        "description" to description,
                        "private" to true,
                        "auto_init" to true,
                    ),
                )
                .retrieve()
                .body(GithubRepositoryApiResponse::class.java)
                ?.toExternal()
        }

    fun createOrUpdateFile(
        token: String,
        repoFullName: String,
        path: String,
        commitMessage: String,
        content: String,
        branch: String,
        sha: String? = null,
    ) {
        val request = linkedMapOf(
            "message" to commitMessage,
            "content" to Base64.getEncoder().encodeToString(content.toByteArray(StandardCharsets.UTF_8)),
            "branch" to branch,
        )
        if (sha != null) {
            request["sha"] = sha
        }
        execute("Failed to write $path in GitHub repository") {
            client.put()
                .uri("/repos/{owner}/{repo}/contents/{path}", owner(repoFullName), repo(repoFullName), path)
                .headers { headers(token, it) }
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity()
            Unit
        }
    }

    fun getFile(token: String?, repoFullName: String, path: String, ref: String): GithubContentFileResponse =
        execute("Failed to load $path from GitHub repository") {
            client.get()
                .uri("/repos/{owner}/{repo}/contents/{path}?ref={ref}", owner(repoFullName), repo(repoFullName), path, ref)
                .headers { headers(token, it) }
                .retrieve()
                .body(GithubContentApiResponse::class.java)
                ?.let {
                    GithubContentFileResponse(
                        sha = it.sha,
                        content = it.content?.replace("\n", "")?.let(::decodeBase64),
                    )
                }
        }

    fun makeRepositoryPublic(token: String, repoFullName: String): GithubRepositoryResponse =
        execute("Failed to publish GitHub repository") {
            client.patch()
                .uri("/repos/{owner}/{repo}", owner(repoFullName), repo(repoFullName))
                .headers { headers(token, it) }
                .contentType(MediaType.APPLICATION_JSON)
                .body(mapOf("private" to false))
                .retrieve()
                .body(GithubRepositoryApiResponse::class.java)
                ?.toExternal()
        }

    fun getPullRequest(token: String?, repoFullName: String, pullRequestNumber: Int): GithubPullRequestResponse =
        execute("Failed to load GitHub pull request") {
            client.get()
                .uri("/repos/{owner}/{repo}/pulls/{pullNumber}", owner(repoFullName), repo(repoFullName), pullRequestNumber)
                .headers { headers(token, it) }
                .retrieve()
                .body(GithubPullRequestApiResponse::class.java)
                ?.toExternal()
        }

    fun cloneUrl(repoFullName: String): String = "$cloneBaseUrl/$repoFullName.git"

    private fun headers(token: String?, headers: HttpHeaders) {
        if (!token.isNullOrBlank()) {
            headers.setBearerAuth(token)
        }
        headers.accept = listOf(MediaType.APPLICATION_JSON)
        headers["X-GitHub-Api-Version"] = "2022-11-28"
    }

    private fun owner(repoFullName: String): String = repoFullName.substringBefore("/")

    private fun repo(repoFullName: String): String = repoFullName.substringAfter("/")

    private fun decodeBase64(value: String): String = String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8)

    private fun <T> execute(message: String, action: () -> T?): T =
        try {
            action() ?: throw ResponseStatusException(HttpStatus.BAD_GATEWAY, message)
        } catch (ex: RestClientResponseException) {
            throw ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "$message: ${ex.responseBodyAsString.ifBlank { ex.statusText }}",
            )
        } catch (ex: ResponseStatusException) {
            throw ex
        } catch (ex: Exception) {
            throw ResponseStatusException(HttpStatus.BAD_GATEWAY, "$message: ${ex.message}")
        }
}

private data class GithubRepositoryApiResponse(
    val name: String,
    val full_name: String,
    val html_url: String,
    val default_branch: String,
    val private: Boolean,
) {
    fun toExternal(): GithubRepositoryResponse =
        GithubRepositoryResponse(
            name = name,
            fullName = full_name,
            htmlUrl = html_url,
            defaultBranch = default_branch,
            private = private,
        )
}

private data class GithubContentApiResponse(
    val sha: String,
    val content: String?,
)

private data class GithubPullRequestApiResponse(
    val number: Int,
    val html_url: String,
    val state: String,
    val head: GithubPullRequestHeadApiResponse,
) {
    fun toExternal(): GithubPullRequestResponse =
        GithubPullRequestResponse(
            number = number,
            htmlUrl = html_url,
            state = state,
            headSha = head.sha,
        )
}

private data class GithubPullRequestHeadApiResponse(
    val sha: String,
)
