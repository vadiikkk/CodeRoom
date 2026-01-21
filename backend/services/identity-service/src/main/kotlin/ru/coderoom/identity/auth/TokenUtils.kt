package ru.coderoom.identity.auth

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

object TokenUtils {
    private val rng = SecureRandom()

    fun newRefreshTokenValue(bytes: Int = 32): String {
        val raw = ByteArray(bytes)
        rng.nextBytes(raw)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw)
    }

    fun sha256Hex(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        return digest.joinToString(separator = "") { b -> "%02x".format(b) }
    }
}
