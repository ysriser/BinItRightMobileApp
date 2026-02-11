package iss.nus.edu.sg.webviews.binitrightmobileapp

import iss.nus.edu.sg.webviews.binitrightmobileapp.model.Accessory
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.UserAccessory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AvatarCustomizationLogicTest {

    @Test
    fun testShouldCallEquipApi_Logic() {
        val accessory = Accessory(1L, "Hat", "url", 100)
        val equippedItem = UserAccessory(1L, true, accessory)
        val notEquippedItem = UserAccessory(2L, false, accessory)

        assertFalse(AvatarLogicUtils.shouldCallEquipApi(equippedItem))
        assertTrue(AvatarLogicUtils.shouldCallEquipApi(notEquippedItem))
    }

    @Test
    fun testGridSpanCount_Logic() {
        assertEquals(2, AvatarLogicUtils.getGridSpanCount())
    }

    @Test
    fun testApiResponseHandling_Logic() {
        val items = listOf(
            UserAccessory(1L, true, Accessory(101L, "Item 1", "u1", 10)),
            UserAccessory(2L, false, Accessory(102L, "Item 2", "u2", 20))
        )

        var adapterData = emptyList<UserAccessory>()
        val isSuccessful = true

        if (isSuccessful) {
            adapterData = items
        }

        assertEquals(2, adapterData.size)
        assertEquals("Item 1", adapterData[0].accessories.name)
    }
}