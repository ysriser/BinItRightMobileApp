package iss.nus.edu.sg.webviews.binitrightmobileapp

import iss.nus.edu.sg.webviews.binitrightmobileapp.model.IssueResponse
import junit.framework.Assert.assertEquals
import org.junit.Test

class IssueReponseTest {

    @Test
    fun issueResponse_holdsIssueId() {
        val response = IssueResponse(issueId = 101L)

        assertEquals(101L, response.issueId)
    }

}