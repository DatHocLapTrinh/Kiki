package com.example.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordHasherTest {

    @Test
    fun `hash generates consistent 64 character sha256 hex string`() {
        val hash1 = PasswordHasher.hash("Secret123")
        val hash2 = PasswordHasher.hash("Secret123")

        assertEquals(64, hash1.length)
        assertEquals(hash1, hash2)
        assertNotEquals("Secret123", hash1)
    }

    @Test
    fun `verify returns true for correct password and false for wrong password`() {
        val password = "SuperSecurePassword99!"
        val hashed = PasswordHasher.hash(password)

        assertTrue(PasswordHasher.verify(password, hashed))
        assertFalse(PasswordHasher.verify("WrongPassword", hashed))
    }
}
