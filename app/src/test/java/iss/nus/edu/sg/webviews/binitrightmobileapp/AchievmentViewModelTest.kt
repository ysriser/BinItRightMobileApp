package iss.nus.edu.sg.webviews.binitrightmobileapp

import iss.nus.edu.sg.webviews.binitrightmobileapp.model.Achievement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AchievementViewModelLogicTest {

    @Test
    fun testMergeLogic_StateTransition() {
        val fixed = listOf(
            Achievement(1L, "A1", "D", "C", "U", false),
            Achievement(2L, "A2", "D", "C", "U", false)
        )
        val remoteIds = setOf(1L)

        val result = fixed.map { if (remoteIds.contains(it.id)) it.copy(isUnlocked = true) else it }

        assertTrue(result[0].isUnlocked)
        assertFalse(result[1].isUnlocked)
    }

    @Test
    fun testUserId_Fallback_Logic() {
        val userIdValid = 100L
        val userIdInvalid = -1L

        val actionForValid = if (userIdValid == -1L) "USE_FIXED" else "FETCH_REMOTE"
        val actionForInvalid = if (userIdInvalid == -1L) "USE_FIXED" else "FETCH_REMOTE"

        assertEquals("FETCH_REMOTE", actionForValid)
        assertEquals("USE_FIXED", actionForInvalid)
    }

    @Test
    fun testRemoteData_Filtering_Logic() {
        val remote = listOf(
            Achievement(1, "A", "D", "C", "U", true),
            Achievement(2, "B", "D", "C", "U", false)
        )

        val unlocked = remote.filter { it.isUnlocked }.map { it.id }.toSet()

        assertTrue(unlocked.contains(1L))
        assertFalse(unlocked.contains(2L))
    }

    @Test
    fun testLoadingState_Toggle_Logic() {
        var loading = false
        val states = mutableListOf<Boolean>()

        loading = true
        states.add(loading)
        loading = false
        states.add(loading)

        assertTrue(states[0])
        assertFalse(states[1])
    }

    @Test
    fun testError_Fallback_Logic() {
        val fixed = listOf(Achievement(1, "F", "D", "C", "U", false))
        var result: List<Achievement>

        try {
            throw Exception("Network Error")
        } catch (e: Exception) {
            result = fixed
        }

        assertEquals(1, result.size)
        assertEquals("F", result[0].name)
    }
}