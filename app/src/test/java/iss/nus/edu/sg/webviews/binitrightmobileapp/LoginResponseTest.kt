package iss.nus.edu.sg.webviews.binitrightmobileapp

import iss.nus.edu.sg.webviews.binitrightmobileapp.model.LoginResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginResponseTest {

    @Test
    fun loginResponse_successWithToken() {
        val response = LoginResponse(
            success = true,
            message = "Login successful",
            token = "jwt.token.here"
        )

        assertTrue(response.success)
        assertEquals("Login successful", response.message)
        assertEquals("jwt.token.here", response.token)
    }

    @Test
    fun loginResponse_failureWithoutToken() {
        val response = LoginResponse(
            success = false,
            message = "Invalid credentials"
        )

        assertFalse(response.success)
        assertEquals("Invalid credentials", response.message)
        assertEquals(null, response.token)
    }
}