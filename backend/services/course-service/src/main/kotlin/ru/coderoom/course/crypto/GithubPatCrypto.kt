package ru.coderoom.course.crypto

import org.springframework.stereotype.Component
import ru.coderoom.course.security.CourseProperties
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

data class EncryptedBytes(
    val iv: ByteArray,
    val ciphertext: ByteArray,
)

@Component
class GithubPatCrypto(
    props: CourseProperties,
) {
    private val keyBytes: ByteArray = sha256(props.githubPatKey)
    private val rng = SecureRandom()

    fun encrypt(plaintext: String): EncryptedBytes {
        val iv = ByteArray(12).also { rng.nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(keyBytes, "AES"), GCMParameterSpec(128, iv))
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return EncryptedBytes(iv = iv, ciphertext = ciphertext)
    }

    fun decrypt(encrypted: EncryptedBytes): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(keyBytes, "AES"), GCMParameterSpec(128, encrypted.iv))
        val plaintext = cipher.doFinal(encrypted.ciphertext)
        return plaintext.toString(Charsets.UTF_8)
    }

    private fun sha256(input: String): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
}
