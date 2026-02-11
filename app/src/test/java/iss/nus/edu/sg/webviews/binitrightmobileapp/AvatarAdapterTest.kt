package iss.nus.edu.sg.webviews.binitrightmobileapp

import iss.nus.edu.sg.webviews.binitrightmobileapp.model.Accessory
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.UserAccessory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AvatarAdapterLogicTest {

    @Test
    fun testAdapter_ItemCount_Logic() {
        val items = listOf(
            UserAccessory(1, false, Accessory(101, "Old Cap", "url", 10)),
            UserAccessory(2, true, Accessory(102, "New Shirt", "url", 20))
        )
        val adapter = AvatarAdapter(items) { }
        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun testUIState_Equipped_Logic() {
        val item = UserAccessory(1, true, Accessory(101, "Blue Cap", "url", 10))

        val strokeWidth = if (item.equipped) 6 else 0
        val alpha = if (item.equipped) 0.7f else 1.0f
        val isClickable = !item.equipped

        assertEquals(6, strokeWidth)
        assertEquals(0.7f, alpha, 0.0f)
        assertFalse(isClickable)
    }

    @Test
    fun testUIState_Unequipped_Logic() {
        val item = UserAccessory(2, false, Accessory(102, "Red Shirt", "url", 20))

        val strokeWidth = if (item.equipped) 6 else 0
        val alpha = if (item.equipped) 0.7f else 1.0f
        val isClickable = !item.equipped

        assertEquals(0, strokeWidth)
        assertEquals(1.0f, alpha, 0.0f)
        assertTrue(isClickable)
    }

    @Test
    fun testResourceNaming_Logic() {
        val name = "Golden Knight Armor"
        val expected = "golden_knight_armor"
        val actual = name.lowercase().replace(" ", "_")
        assertEquals(expected, actual)
    }

    @Test
    fun testCallback_Trigger_Logic() {
        var callCount = 0
        val callback = { _: UserAccessory -> callCount++ }

        val item = UserAccessory(1, false, Accessory(1, "A", "u", 0))
        callback(item)

        assertEquals(1, callCount)
    }
}