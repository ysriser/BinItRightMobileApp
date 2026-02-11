package iss.nus.edu.sg.webviews.binitrightmobileapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AchievementDetailLogicTest {

    @Test
    fun testUIState_Logic() {
        val unlockedState = AchievementLogicUtils.getUIState(true)
        assertEquals("UNLOCKED", unlockedState.statusText)
        assertEquals("#00C853", unlockedState.statusColor)
        assertTrue(unlockedState.isShareEnabled)

        val lockedState = AchievementLogicUtils.getUIState(false)
        assertEquals("LOCKED", lockedState.statusText)
        assertEquals("#78909C", lockedState.statusColor)
        assertFalse(lockedState.isShareEnabled)
    }

    @Test
    fun testUsername_Logic() {
        assertEquals("Achiever", AchievementLogicUtils.getUsername(null))
        assertEquals("Achiever", AchievementLogicUtils.getUsername(""))
        assertEquals("Achiever", AchievementLogicUtils.getUsername("invalid_token"))
    }

    @Test
    fun testShareText_Logic() {
        val text = AchievementLogicUtils.generateShareText("User", "Title", "Desc")
        assertTrue(text.contains("User"))
        assertTrue(text.contains("Title"))
        assertTrue(text.contains("Desc"))
    }
}