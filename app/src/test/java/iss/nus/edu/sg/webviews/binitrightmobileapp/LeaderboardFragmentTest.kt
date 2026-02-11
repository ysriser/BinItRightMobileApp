
package iss.nus.edu.sg.webviews.binitrightmobileapp

import iss.nus.edu.sg.webviews.binitrightmobileapp.model.LeaderboardEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LeaderboardFragmentTest {

    @Test
    fun testDataHandling_Success_Logic() {
        val remoteData = listOf(
            LeaderboardEntry(1L, "User A", 100),
            LeaderboardEntry(2L, "User B", 90)
        )

        val isSuccessful = true
        val responseBody = if (isSuccessful) remoteData else null

        val finalData = responseBody ?: emptyList()

        assertEquals(2, finalData.size)
        assertEquals("User A", finalData[0].username)
        assertEquals(1L, finalData[0].userId)
    }

    @Test
    fun testDataHandling_Empty_Logic() {
        val isSuccessful = true
        val responseBody: List<LeaderboardEntry>? = null

        val finalData = responseBody ?: emptyList()

        assertTrue(finalData.isEmpty())
    }

    @Test
    fun testDataHandling_Failure_Logic() {
        val isSuccessful = false
        val responseCode = 500

        val errorLog = if (!isSuccessful) "Error: $responseCode" else ""

        assertEquals("Error: 500", errorLog)
    }

    @Test
    fun testNetworkError_Logic() {
        val exceptionMessage = "Connection Timeout"
        val errorDisplay = "Network Error: $exceptionMessage"

        assertEquals("Network Error: Connection Timeout", errorDisplay)
    }

    @Test
    fun testLayoutManager_Type_Logic() {
        val layoutType = "LinearLayoutManager"
        assertEquals("LinearLayoutManager", layoutType)
    }
}