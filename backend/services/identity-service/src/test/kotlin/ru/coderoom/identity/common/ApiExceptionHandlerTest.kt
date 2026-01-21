package ru.coderoom.identity.common

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.core.MethodParameter
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.mock.http.MockHttpInputMessage
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.server.ResponseStatusException

class ApiExceptionHandlerTest {
    private val handler = ApiExceptionHandler()

    @Test
    fun handleIllegalArgument_returnsBadRequest_withMessage() {
        val res = handler.handleIllegalArgument(IllegalArgumentException("boom"))
        assertEquals(HttpStatus.BAD_REQUEST, res.statusCode)
        assertEquals(ApiError(message = "boom"), res.body)
    }

    @Test
    fun handleIllegalArgument_whenMessageNull_returnsDefaultMessage() {
        val res = handler.handleIllegalArgument(IllegalArgumentException())
        assertEquals(HttpStatus.BAD_REQUEST, res.statusCode)
        assertEquals(ApiError(message = "Bad request"), res.body)
    }

    @Test
    fun handleUnreadableBody_returnsBadRequest_withFixedMessage() {
        val res = handler.handleUnreadableBody(
            HttpMessageNotReadableException(
                "bad json",
                RuntimeException("cause"),
                MockHttpInputMessage(ByteArray(0)),
            ),
        )
        assertEquals(HttpStatus.BAD_REQUEST, res.statusCode)
        assertEquals(ApiError(message = "Invalid JSON body"), res.body)
    }

    @Test
    fun handleValidation_returnsBadRequest_withFirstFieldErrorMessage() {
        val target = Any()
        val binding = BeanPropertyBindingResult(target, "target").apply {
            addError(FieldError("target", "email", "must be a well-formed email address"))
        }
        val method = Dummy::class.java.getDeclaredMethod("handle", Any::class.java)
        val ex = MethodArgumentNotValidException(MethodParameter(method, 0), binding)

        val res = handler.handleValidation(ex)
        assertEquals(HttpStatus.BAD_REQUEST, res.statusCode)
        assertEquals(ApiError(message = "must be a well-formed email address"), res.body)
    }

    @Test
    fun handleResponseStatus_returnsProvidedStatusAndReason() {
        val res = handler.handleResponseStatus(ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"))
        assertEquals(HttpStatus.NOT_FOUND, res.statusCode)
        assertEquals(ApiError(message = "User not found"), res.body)
    }

    @Test
    fun handleAuth_returnsUnauthorized_withFixedMessage() {
        val res = handler.handleAuth(BadCredentialsException("bad creds"))
        assertEquals(HttpStatus.UNAUTHORIZED, res.statusCode)
        assertEquals(ApiError(message = "Unauthorized"), res.body)
    }

    @Test
    fun handleAccessDenied_returnsForbidden_withFixedMessage() {
        val res = handler.handleAccessDenied(AccessDeniedException("nope"))
        assertEquals(HttpStatus.FORBIDDEN, res.statusCode)
        assertEquals(ApiError(message = "Forbidden"), res.body)
    }

    private class Dummy {
        @Suppress("UNUSED_PARAMETER")
        fun handle(arg: Any) = Unit
    }
}
