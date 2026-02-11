package iss.nus.edu.sg.webviews.binitrightmobileapp.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessoryTest {

    @Test
    fun testAccessory_ConstructorAndPropertyAccess() {
        val accessory = Accessory(
            accessoriesId = 101L,
            name = "Blue Cap",
            imageUrl = "cap_blue_url",
            requiredPoints = 50,
            owned = true,
            equipped = false
        )

        assertEquals(101L, accessory.accessoriesId)
        assertEquals("Blue Cap", accessory.name)
        assertEquals("cap_blue_url", accessory.imageUrl)
        assertEquals(50, accessory.requiredPoints)
        assertTrue(accessory.owned)
        assertFalse(accessory.equipped)
    }

    @Test
    fun testAccessory_DefaultValues() {
        val accessory = Accessory(
            accessoriesId = 102L,
            name = "Golden Armor",
            imageUrl = "armor_gold_url",
            requiredPoints = 500
        )

        assertFalse(accessory.owned)
        assertFalse(accessory.equipped)
    }

    @Test
    fun testAccessory_DataClassCopy() {
        val original = Accessory(
            accessoriesId = 103L,
            name = "Simple Shirt",
            imageUrl = "shirt_url",
            requiredPoints = 20,
            owned = false,
            equipped = false
        )

        val updated = original.copy(owned = true, equipped = true)

        assertEquals(103L, updated.accessoriesId)
        assertEquals("Simple Shirt", updated.name)
        assertTrue(updated.owned)
        assertTrue(updated.equipped)
    }

    @Test
    fun testAccessory_Equality() {
        val a1 = Accessory(1L, "Item", "url", 10)
        val a2 = Accessory(1L, "Item", "url", 10)
        val a3 = Accessory(2L, "Item", "url", 10)

        assertEquals(a1, a2)
        assertTrue(a1.hashCode() == a2.hashCode())
        assertTrue(a1 != a3)
    }
}