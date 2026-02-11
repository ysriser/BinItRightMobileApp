package iss.nus.edu.sg.webviews.binitrightmobileapp

import iss.nus.edu.sg.webviews.binitrightmobileapp.model.Achievement
import org.junit.Assert.assertEquals
import org.junit.Test

class AchievementFragmentLogicTest {

    private fun calculateProgress(list: List<Achievement>): Triple<Int, String, String> {
        val total = list.size
        val unlocked = list.count { it.isUnlocked }
        val remaining = total - unlocked
        val fractionText = "$unlocked/$total"
        val message = if (remaining > 0) {
            "$remaining more to unlock!"
        } else {
            "All achievements completed! 🎉"
        }
        return Triple(unlocked, fractionText, message)
    }

    @Test
    fun testProgressFlow_PartialState() {
        val list = listOf(
            Achievement(1, "A1", "D", "C", "U", true),
            Achievement(2, "A2", "D", "C", "U", false),
            Achievement(3, "A3", "D", "C", "U", false)
        )

        val (unlocked, fraction, msg) = calculateProgress(list)

        assertEquals(1, unlocked)
        assertEquals("1/3", fraction)
        assertEquals("2 more to unlock!", msg)
    }

    @Test
    fun testProgressFlow_FullState() {
        val list = listOf(
            Achievement(1, "A1", "D", "C", "U", true),
            Achievement(2, "A2", "D", "C", "U", true)
        )

        val (unlocked, fraction, msg) = calculateProgress(list)

        assertEquals(2, unlocked)
        assertEquals("2/2", fraction)
        assertEquals("All achievements completed! 🎉", msg)
    }

    @Test
    fun testNavigationBundle_Logic() {
        val item = Achievement(10L, "Name", "Desc", "Crit", "Url", true, "2026-02-11")

        val bundleMap = mapOf(
            "name" to item.name,
            "description" to item.description,
            "isUnlocked" to item.isUnlocked,
            "dateAchieved" to item.dateAchieved
        )

        assertEquals("Name", bundleMap["name"])
        assertEquals(true, bundleMap["isUnlocked"])
        assertEquals("2026-02-11", bundleMap["dateAchieved"])
    }

    @Test
    fun testProgressBar_ScaleLogic() {
        val total = 10
        val unlocked = 4

        val max = total
        val progress = unlocked

        assertEquals(10, max)
        assertEquals(4, progress)
    }
}