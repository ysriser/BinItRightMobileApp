
package iss.nus.edu.sg.webviews.binitrightmobileapp.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class LeaderboardEntryTest {

    @Test
    fun testLeaderboardEntry_ConstructorAndAccessors() {
        val alias = buildAlias("Eco", "Warrior")
        val entry = LeaderboardEntry(123L, alias, 50)

        assertEquals(123L, entry.userId)
        assertEquals(alias, entry.username)
        assertEquals(50, entry.totalQuantity)
    }

    @Test
    fun testLeaderboardEntry_Equality() {
        val alias = buildAlias("User", "One")
        val entry1 = LeaderboardEntry(1L, alias, 10)
        val entry2 = LeaderboardEntry(1L, alias, 10)
        val entry3 = LeaderboardEntry(2L, alias, 10)

        assertEquals(entry1, entry2)
        assertEquals(entry1.hashCode(), entry2.hashCode())
        assertNotEquals(entry1, entry3)
    }

    @Test
    fun testLeaderboardEntry_Copy() {
        val alias = buildAlias("Original", "Name")
        val original = LeaderboardEntry(1L, alias, 100)
        val updated = original.copy(totalQuantity = 150)

        assertEquals(1L, updated.userId)
        assertEquals(alias, updated.username)
        assertEquals(150, updated.totalQuantity)
    }

    @Test
    fun testLeaderboardEntry_ToString() {
        val alias = buildAlias("User", "View")
        val entry = LeaderboardEntry(1L, alias, 10)
        val toString = entry.toString()

        assert(toString.contains("userId=1"))
        assert(toString.contains("username=$alias"))
        assert(toString.contains("totalQuantity=10"))
    }

    private fun buildAlias(left: String, right: String): String = left + right
}
