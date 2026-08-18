package com.networkguardian.domain

import com.networkguardian.security.PinHasher
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinHasherTest {
    @Test
    fun `verify succeeds for correct pin`() {
        val salt = PinHasher.generateSalt()
        val hash = PinHasher.hash("1234", salt)
        assertTrue(PinHasher.verify("1234", salt, hash))
    }

    @Test
    fun `verify fails for incorrect pin`() {
        val salt = PinHasher.generateSalt()
        val hash = PinHasher.hash("1234", salt)
        assertFalse(PinHasher.verify("0000", salt, hash))
    }

    @Test
    fun `hash is never the plaintext pin`() {
        val salt = PinHasher.generateSalt()
        val hash = PinHasher.hash("1234", salt)
        assertNotEquals("1234", hash)
    }
}
