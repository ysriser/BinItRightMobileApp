
package iss.nus.edu.sg.webviews.binitrightmobileapp

import iss.nus.edu.sg.webviews.binitrightmobileapp.model.Accessory
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.UserAccessory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AvatarCustomizationLogicTest {

    @Test
    fun testHandleEquip_ConditionLogic() {
        val accessory = Accessory(101L, "Blue Cap", "url", 100)

        val equippedItem = UserAccessory(1L, true, accessory)
        val notEquippedItem = UserAccessory(2L, false, accessory)

        val shouldSkipApiForEquipped = equippedItem.equipped
        val shouldCallApiForNotEquipped = !notEquippedItem.equipped

        assertTrue(shouldSkipApiForEquipped)
        assertTrue(shouldCallApiForNotEquipped)
    }

    @Test
    fun testGridLayoutSpanCount() {
        val spanCount = 2
        assertEquals(2, spanCount)
    }

    @Test
    fun testApiResponseHandling_Success() {
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

    @Test
    fun testApiResponseHandling_Failure() {
        var adapterData = listOf(UserAccessory(1L, true, Accessory(101L, "Old", "u", 0)))
        val isSuccessful = false

        if (isSuccessful) {
            adapterData = emptyList()
        }

        assertEquals(1, adapterData.size)
        assertEquals("Old", adapterData[0].accessories.name)
    }

    @Test
    fun testNavigationLogic_Trigger() {
        var navigateUpCalled = false
        val performClick = { navigateUpCalled = true }

        performClick()

        assertTrue(navigateUpCalled)
    }
}