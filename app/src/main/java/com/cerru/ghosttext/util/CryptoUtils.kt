package com.cerru.ghosttext.util

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

data class CryptoPayload(
    val cipherTextBase64: String,
    val ivBase64: String,
    val authTagBase64: String
)

object CryptoUtils {
    private const val KEY_LENGTH = 256
    private const val ITERATIONS = 100_000
    private const val TAG_LENGTH = 128

    fun deriveKey(password: String, salt: ByteArray): SecretKey {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val key = factory.generateSecret(spec).encoded
        return SecretKeySpec(key, "AES")
    }

    fun encrypt(plainText: String, password: String, saltBase64: String): CryptoPayload {
        val salt = Base64.decode(saltBase64, Base64.DEFAULT)
        val key = deriveKey(password, salt)
        val iv = ByteArray(12)
        SecureRandom().nextBytes(iv)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_LENGTH, iv))
        val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val cipherText = cipherBytes.copyOfRange(0, cipherBytes.size - TAG_LENGTH / 8)
        val tag = cipherBytes.copyOfRange(cipherBytes.size - TAG_LENGTH / 8, cipherBytes.size)
        return CryptoPayload(
            cipherTextBase64 = Base64.encodeToString(cipherText, Base64.NO_WRAP),
            ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP),
            authTagBase64 = Base64.encodeToString(tag, Base64.NO_WRAP)
        )
    }

    fun decrypt(cipherTextBase64: String, ivBase64: String, authTagBase64: String, password: String, saltBase64: String): String {
        val salt = Base64.decode(saltBase64, Base64.DEFAULT)
        val key = deriveKey(password, salt)
        val iv = Base64.decode(ivBase64, Base64.DEFAULT)
        val cipherText = Base64.decode(cipherTextBase64, Base64.DEFAULT)
        val tag = Base64.decode(authTagBase64, Base64.DEFAULT)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_LENGTH, iv))
        val combined = cipherText + tag
        val result = cipher.doFinal(combined)
        return result.toString(Charsets.UTF_8)
    }
}
