package iss.nus.edu.sg.webviews.binitrightmobileapp.model
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AchievementTest {

    @Test
    fun testAchievement_ConstructorAndPropertyAccess() {
        val achievement = Achievement(
            id = 1L,
            name = "Recycle Rookie",
            description = "Your first step to save the earth",
            criteria = "Recycle 1 item",
            badgeIconUrl = "http://example.com/icon.png"
        )

        assertEquals(1L, achievement.id)
        assertEquals("Recycle Rookie", achievement.name)
        assertEquals("Your first step to save the earth", achievement.description)
        assertEquals("Recycle 1 item", achievement.criteria)
        assertEquals("http://example.com/icon.png", achievement.badgeIconUrl)
    }

    @Test
    fun testAchievement_DefaultValues() {
        val achievement = Achievement(
            id = 2L,
            name = "Eco Warrior",
            description = "Description",
            criteria = "Criteria",
            badgeIconUrl = "url"
        )

        assertFalse(achievement.isUnlocked)
        assertNull(achievement.dateAchieved)
    }

    @Test
    fun testAchievement_UpdateStatus() {
        val achievement = Achievement(
            id = 3L,
            name = "Green Master",
            description = "Desc",
            criteria = "Crit",
            badgeIconUrl = "url"
        )

        achievement.isUnlocked = true
        achievement.dateAchieved = "2026-02-11"

        assertTrue(achievement.isUnlocked)
        assertEquals("2026-02-11", achievement.dateAchieved)
    }

    @Test
    fun testAchievement_DataClassCopy() {
        val original = Achievement(
            id = 4L,
            name = "Old Name",
            description = "Desc",
            criteria = "Crit",
            badgeIconUrl = "url"
        )

        val copied = original.copy(name = "New Name")

        assertEquals(4L, copied.id)
        assertEquals("New Name", copied.name)
        assertEquals(original.description, copied.description)
    }
}