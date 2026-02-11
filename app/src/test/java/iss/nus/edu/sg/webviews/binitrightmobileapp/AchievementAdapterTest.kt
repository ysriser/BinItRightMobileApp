
package iss.nus.edu.sg.webviews.binitrightmobileapp

import iss.nus.edu.sg.webviews.binitrightmobileapp.model.Achievement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AchievementAdapterTest {

    @Test
    fun testDiffCallback_areItemsTheSame_sameId() {
        val callback = AchievementAdapter.AchievementDiffCallback()
        val a1 = Achievement(101L, "Rookie", "Desc", "Crit", "url", false)
        val a2 = Achievement(101L, "Rookie Updated", "Desc", "Crit", "url", true)
        assertTrue(callback.areItemsTheSame(a1, a2))
    }

    @Test
    fun testDiffCallback_areItemsTheSame_differentId() {
        val callback = AchievementAdapter.AchievementDiffCallback()
        val a1 = Achievement(101L, "A", "D", "C", "U", false)
        val a2 = Achievement(102L, "A", "D", "C", "U", false)
        assertFalse(callback.areItemsTheSame(a1, a2))
    }

    @Test
    fun testDiffCallback_areContentsTheSame_identical() {
        val callback = AchievementAdapter.AchievementDiffCallback()
        val a1 = Achievement(1L, "A", "B", "C", "D", true)
        val a2 = Achievement(1L, "A", "B", "C", "D", true)
        assertTrue(callback.areContentsTheSame(a1, a2))
    }

    @Test
    fun testDiffCallback_areContentsTheSame_differentStatus() {
        val callback = AchievementAdapter.AchievementDiffCallback()
        val a1 = Achievement(1L, "A", "B", "C", "D", true)
        val a2 = Achievement(1L, "A", "B", "C", "D", false)
        assertFalse(callback.areContentsTheSame(a1, a2))
    }

    @Test
    fun testAdapterCallback_lambdaExecution() {
        var captured: Achievement? = null
        val testItem = Achievement(5L, "Test", "Test", "Test", "Test", true)

        val clickListener = { item: Achievement -> captured = item }
        clickListener(testItem)

        assertEquals(5L, captured?.id)
        assertEquals("Test", captured?.name)
    }
}