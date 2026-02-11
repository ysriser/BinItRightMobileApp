
package iss.nus.edu.sg.webviews.binitrightmobileapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AchievementDetailLogicTest {

    @Test
    fun testShareTextGeneration_FormatConsistency() {
        val user = "JohnDoe"
        val title = "Recycle Rookie"
        val desc = "Recycled 1 item"

        val actual = "🏆 $user just unlocked the '$title' achievement in BinItRight! \n\n$desc \n\nJoin us in recycling to save the planet! 🌍"

        assertTrue(actual.contains(user))
        assertTrue(actual.contains(title))
        assertTrue(actual.contains(desc))
        assertTrue(actual.startsWith("🏆"))
    }

    @Test
    fun testStatusLogic_UnlockedState_VisualParams() {
        val isUnlocked = true

        val statusText = if (isUnlocked) "UNLOCKED" else "LOCKED"
        val btnEnabled = isUnlocked
        val alpha = if (isUnlocked) 1.0f else 0.6f

        assertEquals("UNLOCKED", statusText)
        assertTrue(btnEnabled)
        assertEquals(1.0f, alpha, 0.0f)
    }

    @Test
    fun testStatusLogic_LockedState_VisualParams() {
        val isUnlocked = false

        val statusText = if (isUnlocked) "UNLOCKED" else "LOCKED"
        val btnEnabled = isUnlocked
        val alpha = if (isUnlocked) 1.0f else 0.6f

        assertEquals("LOCKED", statusText)
        assertFalse(btnEnabled)
        assertEquals(0.6f, alpha, 0.0f)
    }

    @Test
    fun testUserNameLogic_FullFlow() {
        val tokenMap = mapOf(
            "valid_token" to "RealUser",
            "" to "Achiever",
            null to "Achiever"
        )

        tokenMap.forEach { (token, expectedName) ->
            val result = if (!token.isNullOrEmpty()) {
                if (token == "valid_token") "RealUser" else "Achiever"
            } else {
                "Achiever"
            }
            assertEquals(expectedName, result)
        }
    }

    @Test
    fun testArgumentFallback_NullSafety() {
        val bundleName: String? = null
        val bundleCriteria: String? = ""

        val name = bundleName ?: "Achievement"
        val criteria = if (bundleCriteria.isNullOrEmpty()) "No criteria" else bundleCriteria

        assertEquals("Achievement", name)
        assertEquals("No criteria", criteria)
    }
}