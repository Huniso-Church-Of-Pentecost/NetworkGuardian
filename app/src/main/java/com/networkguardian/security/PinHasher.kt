package com.networkguardian.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/**
 * PBKDF2-style salted hashing for the app-lock PIN. The PIN itself is never stored — only
 * a salt + hash pair, matching standard practice for local credential storage.
 */
object PinHasher {
    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH = 256

    fun generateSalt(): String {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        return Base64.getEncoder().encodeToString(salt)
    }

    fun hash(pin: String, saltBase64: String): String {
        val salt = Base64.getDecoder().decode(saltBase64)
        val spec = javax.crypto.spec.PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = factory.generateSecret(spec).encoded
        return Base64.getEncoder().encodeToString(hash)
    }

    fun verify(pin: String, saltBase64: String, expectedHash: String): Boolean {
        val computed = hash(pin, saltBase64)
        return MessageDigest.isEqual(computed.toByteArray(), expectedHash.toByteArray())
    }
}
