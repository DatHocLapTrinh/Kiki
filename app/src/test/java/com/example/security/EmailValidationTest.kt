package com.example.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.regex.Pattern

class EmailValidationTest {

    private val emailPattern = Pattern.compile(
        "[a-zA-Z0-9+._%\\-]{1,256}" +
        "@" +
        "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
        "(" +
        "\\." +
        "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
        ")+"
    )

    @Test
    fun `validates diverse educational and public emails`() {
        assertTrue(emailPattern.matcher("student@fpt.edu.vn").matches())
        assertTrue(emailPattern.matcher("user@gmail.com").matches())
        assertTrue(emailPattern.matcher("learner.kiki@outlook.com").matches())
        assertTrue(emailPattern.matcher("datphamtop1@gmail.com").matches())
    }

    @Test
    fun `rejects invalid emails`() {
        assertFalse(emailPattern.matcher("plainaddress").matches())
        assertFalse(emailPattern.matcher("@missingusername.com").matches())
        assertFalse(emailPattern.matcher("missingdomain@.com").matches())
    }
}
