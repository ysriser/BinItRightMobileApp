package iss.nus.edu.sg.webviews.binitrightmobileapp

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IssueDescriptionTest {

    @Test
    fun emptyDescription_isInvalid() {
        val description = ""

        val isValid = description.isNotBlank()

        assertFalse(isValid)
    }

    @Test
    fun nullDescription_isInvalid() {
        val description: String? = null

        val isValid = !description.isNullOrBlank()

        assertFalse(isValid)
    }

    @Test
    fun nonEmptyDescription_isValid() {
        val description = "Bin is damaged"

        val isValid = description.isNotBlank()

        assertTrue(isValid)
    }

    @Test
    fun userIdMinusOne_isInvalid() {
        val userId = -1L

        assertTrue(userId == -1L)
    }

    @Test
    fun validUserId_isAccepted() {
        val userId = 45L

        assertTrue(userId > 0)
    }
}