package iss.nus.edu.sg.webviews.binitrightmobileapp

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserRegisterTest {

    @Test
    fun emptyUsername_isInvalid() {
        val username = ""
        val password = "secret123"
        val confirm = "secret123"

        val isValid = username.isNotBlank()
                && password.length >= 6
                && password == confirm

        assertFalse(isValid)
    }

    @Test
    fun passwordTooShort_isInvalid() {
        val username = "alice"
        val password = "123"
        val confirm = "123"

        val isValid = username.isNotBlank()
                && password.length >= 6
                && password == confirm

        assertFalse(isValid)
    }

    @Test
    fun passwordMismatch_isInvalid() {
        val username = "alice"
        val password = "secret123"
        val confirm = "different"

        val isValid = username.isNotBlank()
                && password.length >= 6
                && password == confirm

        assertFalse(isValid)
    }

    @Test
    fun validUsernameAndPassword_isValid() {
        val username = "alice"
        val password = "secret123"
        val confirm = "secret123"

        val isValid = username.isNotBlank()
                && password.length >= 6
                && password == confirm

        assertTrue(isValid)
    }
}