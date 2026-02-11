package iss.nus.edu.sg.webviews.binitrightmobileapp

import iss.nus.edu.sg.webviews.binitrightmobileapp.model.RegisterRequest
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.RegisterResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserRegisterModelTest {

    @Test
    fun registerRequest_holdsUsernameAndPassword() {
        val request = RegisterRequest(
            username = "alice",
            password = "secret123"
        )

        assertEquals("alice", request.username)
        assertEquals("secret123", request.password)
    }

    @Test
    fun registerResponse_successCase() {
        val response = RegisterResponse(
            success = true,
            message = "Account created"
        )

        assertTrue(response.success)
        assertEquals("Account created", response.message)
    }

    @Test
    fun registerResponse_failureCase() {
        val response = RegisterResponse(
            success = false,
            message = "Username already exists"
        )

        assertFalse(response.success)
        assertEquals("Username already exists", response.message)
    }
}