package ru.coderoom.identity.auth

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TokenUtilsTest {
    @Test
    fun sha256Hex_returnsKnownDigest() {
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            TokenUtils.sha256Hex("abc"),
        )
    }

    @Test
    fun newRefreshTokenValue_isUrlSafe_andWithoutPadding() {
        val token = TokenUtils.newRefreshTokenValue(bytes = 32)

        assertTrue(token.isNotBlank())
        assertFalse(token.contains("="))
        assertEquals(43, token.length)
    }
}
