package iss.nus.edu.sg.webviews.binitrightmobileapp

import iss.nus.edu.sg.webviews.binitrightmobileapp.model.RegisterRequest
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.RegisterResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserRegisterModelTest {

    @Test
    fun registerRequest_holdsUsernameEmailAndPassword() {
        val sampleUser = "recycler_test_user"
        val sampleEmail = "recycler.test@example.com"
        val samplePass = "unit_test_pass"
        val request = RegisterRequest(
            sampleUser,
            sampleEmail,
            samplePass
        )

        assertEquals(sampleUser, request.username)
        assertEquals(sampleEmail, request.emailAddress)
        assertEquals(samplePass, request.password)
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
