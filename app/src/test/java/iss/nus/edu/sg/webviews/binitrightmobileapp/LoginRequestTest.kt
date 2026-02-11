package iss.nus.edu.sg.webviews.binitrightmobileapp

import iss.nus.edu.sg.webviews.binitrightmobileapp.model.LoginRequest
import org.junit.Assert.assertEquals
import org.junit.Test

class LoginRequestTest {

    @Test
    fun loginRequest_holdsUsernameAndPassword() {
        val request = LoginRequest(
            username = "john",
            password = "secret123"
        )

        assertEquals("john", request.username)
        assertEquals("secret123", request.password)
    }
}