package iss.nus.edu.sg.webviews.binitrightmobileapp.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class LeaderboardEntryTest {

    @Test
    fun testLeaderboardEntry_ConstructorAndAccessors() {
        val entry = LeaderboardEntry(
            userId = 123L,
            username = "EcoWarrior",
            totalQuantity = 50
        )

        assertEquals(123L, entry.userId)
        assertEquals("EcoWarrior", entry.username)
        assertEquals(50, entry.totalQuantity)
    }

    @Test
    fun testLeaderboardEntry_Equality() {
        val entry1 = LeaderboardEntry(1L, "User", 10)
        val entry2 = LeaderboardEntry(1L, "User", 10)
        val entry3 = LeaderboardEntry(2L, "User", 10)

        assertEquals(entry1, entry2)
        assertEquals(entry1.hashCode(), entry2.hashCode())
        assertNotEquals(entry1, entry3)
    }

    @Test
    fun testLeaderboardEntry_Copy() {
        val original = LeaderboardEntry(1L, "Original", 100)
        val updated = original.copy(totalQuantity = 150)

        assertEquals(1L, updated.userId)
        assertEquals("Original", updated.username)
        assertEquals(150, updated.totalQuantity)
    }

    @Test
    fun testLeaderboardEntry_ToString() {
        val entry = LeaderboardEntry(1L, "User", 10)
        val toString = entry.toString()

        assert(toString.contains("userId=1"))
        assert(toString.contains("username=User"))
        assert(toString.contains("totalQuantity=10"))
    }
}