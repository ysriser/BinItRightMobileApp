package iss.nus.edu.sg.webviews.binitrightmobileapp

import iss.nus.edu.sg.webviews.binitrightmobileapp.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UserProfileModelTest {

    @Test
    fun userProfile_holdsAllValuesCorrectly() {
        val profile = UserProfile(
            name = "John",
            pointBalance = 1500,
            equippedAvatarName = "Blue Cap",
            totalRecycled = 120,
            aiSummary = "You are an eco warrior!",
            totalAchievement = 5,
            carbonEmissionSaved = 12.75
        )

        assertEquals("John", profile.name)
        assertEquals(1500, profile.pointBalance)
        assertEquals("Blue Cap", profile.equippedAvatarName)
        assertEquals(120, profile.totalRecycled)
        assertEquals("You are an eco warrior!", profile.aiSummary)
        assertEquals(5, profile.totalAchievement)
        assertEquals(12.75, profile.carbonEmissionSaved, 0.0)
    }

    @Test
    fun userProfile_equalityWorksCorrectly() {
        val profile1 = createSample()
        val profile2 = createSample()

        assertEquals(profile1, profile2)
        assertEquals(profile1.hashCode(), profile2.hashCode())
    }

    @Test
    fun userProfile_copyCreatesModifiedObject() {
        val original = createSample()

        val updated = original.copy(pointBalance = 2000)

        assertEquals("John", updated.name)
        assertEquals(2000, updated.pointBalance)
        assertNotEquals(original.pointBalance, updated.pointBalance)
    }

    @Test
    fun userProfile_toString_containsName() {
        val profile = createSample()

        val str = profile.toString()

        assertTrue(str.contains("John"))
        assertTrue(str.contains("1500"))
    }

    @Test
    fun userProfile_sortByPointsDescending() {
        val profiles = listOf(
            createSample().copy(name = "A", pointBalance = 100),
            createSample().copy(name = "B", pointBalance = 500)
        )

        val sorted = profiles.sortedByDescending { it.pointBalance }

        assertEquals("B", sorted.first().name)
    }


    private fun createSample() = UserProfile(
        name = "John",
        pointBalance = 1500,
        equippedAvatarName = "Blue Cap",
        totalRecycled = 120,
        aiSummary = "You are an eco warrior!",
        totalAchievement = 5,
        carbonEmissionSaved = 12.75
    )


}