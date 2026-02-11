package iss.nus.edu.sg.webviews.binitrightmobileapp

import iss.nus.edu.sg.webviews.binitrightmobileapp.model.LoginRequest
import org.junit.Assert.assertEquals
import org.junit.Test

class LoginRequestTest {

    @Test
    fun loginRequest_holdsUsernameAndPassword() {
        val accountInput = buildInput("u", "42")
        val authInput = buildInput("p", "42")
        val request = LoginRequest(accountInput, authInput)

        assertEquals(accountInput, request.username)
        assertEquals(authInput, request.password)
    }

    private fun buildInput(left: String, right: String): String = left + right
}
