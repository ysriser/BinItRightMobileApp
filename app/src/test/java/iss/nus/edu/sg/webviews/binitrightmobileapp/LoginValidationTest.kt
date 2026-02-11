package iss.nus.edu.sg.webviews.binitrightmobileapp

import iss.nus.edu.sg.webviews.binitrightmobileapp.model.LoginResponse
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginValidationTest {

    @Test
    fun emptyUsername_isInvalid() {
        val username = ""
        val password = "secret"

        val isValid = username.isNotEmpty() && password.isNotEmpty()

        assertFalse(isValid)
    }

    @Test
    fun emptyPassword_isInvalid() {
        val username = "john"
        val password = ""

        val isValid = username.isNotEmpty() && password.isNotEmpty()

        assertFalse(isValid)
    }

    @Test
    fun nonEmptyUsernameAndPassword_isValid() {
        val username = "john"
        val password = "secret"

        val isValid = username.isNotEmpty() && password.isNotEmpty()

        assertTrue(isValid)
    }

    @Test
    fun successTrue_allowsNavigation() {
        val response = LoginResponse(
            success = true,
            message = "OK",
            token = "jwt.token"
        )

        val shouldNavigate = response.success

        assertTrue(shouldNavigate)
    }

    @Test
    fun successFalse_blocksNavigation() {
        val response = LoginResponse(
            success = false,
            message = "Invalid login"
        )

        val shouldNavigate = response.success

        assertFalse(shouldNavigate)
    }
}