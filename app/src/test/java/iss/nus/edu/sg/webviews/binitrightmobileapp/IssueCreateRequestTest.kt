package iss.nus.edu.sg.webviews.binitrightmobileapp

import iss.nus.edu.sg.webviews.binitrightmobileapp.model.IssueCreateRequest
import org.junit.Assert.assertEquals
import org.junit.Test

class IssueCreateRequestTest {

    @Test
    fun issueCreateRequest_holdsCorrectValues() {
        val request = IssueCreateRequest(
            issueCategory = "BIN_ISSUES",
            description = "Bin is overflowing"
        )

        assertEquals("BIN_ISSUES", request.issueCategory)
        assertEquals("Bin is overflowing", request.description)
    }
}
