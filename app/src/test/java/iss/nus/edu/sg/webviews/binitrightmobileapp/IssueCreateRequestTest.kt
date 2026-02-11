package iss.nus.edu.sg.webviews.binitrightmobileapp

import iss.nus.edu.sg.webviews.binitrightmobileapp.model.IssueCreateRequest
import org.junit.Assert.assertEquals
import org.junit.Test

class IssueCreateRequestTest {

    @Test
    fun issueCreateRequest_holdsCorrectValues() {
        val request = IssueCreateRequest(
            issueCategory = "BinIssues",
            description = "Bin is overflowing"
        )

        assertEquals("BinIssues", request.issueCategory)
        assertEquals("Bin is overflowing", request.description)
    }
}