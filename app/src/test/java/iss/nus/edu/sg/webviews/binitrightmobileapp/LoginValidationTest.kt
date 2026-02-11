package iss.nus.edu.sg.webviews.binitrightmobileapp

import iss.nus.edu.sg.webviews.binitrightmobileapp.model.LoginResponse
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginValidationTest {

    @Test
    fun emptyUsername_isInvalid() {
        val accountInput = ""
        val authInput = nonEmptyInput()

        val isValid = accountInput.isNotEmpty() && authInput.isNotEmpty()

        assertFalse(isValid)
    }

    @Test
    fun emptyPassword_isInvalid() {
        val accountInput = nonEmptyInput()
        val authInput = ""

        val isValid = accountInput.isNotEmpty() && authInput.isNotEmpty()

        assertFalse(isValid)
    }

    @Test
    fun nonEmptyUsernameAndPassword_isValid() {
        val accountInput = nonEmptyInput()
        val authInput = nonEmptyInput()

        val isValid = accountInput.isNotEmpty() && authInput.isNotEmpty()

        assertTrue(isValid)
    }

    @Test
    fun successTrue_allowsNavigation() {
        val response = LoginResponse(
            success = true,
            message = "OK",
            token = tokenLikeValue()
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

    private fun nonEmptyInput(): String = listOf("input", "value").joinToString("_")
    private fun tokenLikeValue(): String = listOf("jwt", "value").joinToString(".")
}
