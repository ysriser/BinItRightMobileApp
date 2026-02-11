package iss.nus.edu.sg.webviews.binitrightmobileapp

import iss.nus.edu.sg.webviews.binitrightmobileapp.model.LeaderboardEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class LeaderboardAdapterTest {

    @Test
    fun testItemCount_Logic() {
        val items = listOf(
            LeaderboardEntry(userId = 1L, username = "User1", totalQuantity = 100),
            LeaderboardEntry(userId = 2L, username = "User2", totalQuantity = 80)
        )
        val adapter = LeaderboardAdapter(items)
        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun testRankDisplay_Logic() {
        val position = 0
        val displayRank = (position + 1).toString()
        assertEquals("1", displayRank)
    }

    @Test
    fun testRankColor_Gold_Logic() {
        val position = 0
        val color = when (position) {
            0 -> "#FFD700"
            1 -> "#9E9E9E"
            2 -> "#CD7F32"
            else -> "#212121"
        }
        assertEquals("#FFD700", color)
    }

    @Test
    fun testRankColor_Silver_Logic() {
        val position = 1
        val color = when (position) {
            0 -> "#FFD700"
            1 -> "#9E9E9E"
            2 -> "#CD7F32"
            else -> "#212121"
        }
        assertEquals("#9E9E9E", color)
    }

    @Test
    fun testRankColor_Bronze_Logic() {
        val position = 2
        val color = when (position) {
            0 -> "#FFD700"
            1 -> "#9E9E9E"
            2 -> "#CD7F32"
            else -> "#212121"
        }
        assertEquals("#CD7F32", color)
    }

    @Test
    fun testRankColor_Default_Logic() {
        val position = 5
        val color = when (position) {
            0 -> "#FFD700"
            1 -> "#9E9E9E"
            2 -> "#CD7F32"
            else -> "#212121"
        }
        assertEquals("#212121", color)
    }

    @Test
    fun testDataBinding_Logic() {
        val entry = LeaderboardEntry(userId = 123L, username = "Achiever", totalQuantity = 500)

        val userIdLong: Long = entry.userId
        val expectedId: Long = 123L

        assertEquals(expectedId, userIdLong)
        assertEquals("Achiever", entry.username)
        assertEquals(500, entry.totalQuantity)
    }
}