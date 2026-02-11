
package iss.nus.edu.sg.webviews.binitrightmobileapp.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserAccessoryTest {

    @Test
    fun testUserAccessory_ConstructorAndNestedAccess() {
        val accessory = Accessory(
            accessoriesId = 101L,
            name = "Blue Cap",
            imageUrl = "url_cap",
            requiredPoints = 100
        )

        val userAccessory = UserAccessory(
            userAccessoriesId = 1L,
            equipped = true,
            accessories = accessory
        )

        assertEquals(1L, userAccessory.userAccessoriesId)
        assertTrue(userAccessory.equipped)

        assertEquals(101L, userAccessory.accessories.accessoriesId)
        assertEquals("Blue Cap", userAccessory.accessories.name)
    }

    @Test
    fun testUserAccessory_UpdateEquippedStatus() {
        val accessory = Accessory(202L, "Red Shirt", "url_shirt", 200)
        val userAccessory = UserAccessory(2L, false, accessory)

        assertFalse(userAccessory.equipped)

        val updatedUserAccessory = userAccessory.copy(equipped = true)

        assertTrue(updatedUserAccessory.equipped)
        assertEquals(2L, updatedUserAccessory.userAccessoriesId)
        assertEquals("Red Shirt", updatedUserAccessory.accessories.name)
    }

    @Test
    fun testUserAccessory_Equality() {
        val accessory = Accessory(303L, "Gloves", "url_gloves", 50)

        val ua1 = UserAccessory(10L, true, accessory)
        val ua2 = UserAccessory(10L, true, accessory)
        val ua3 = UserAccessory(11L, true, accessory)

        assertEquals(ua1, ua2)
        assertTrue(ua1.hashCode() == ua2.hashCode())
        assertTrue(ua1 != ua3)
    }

    @Test
    fun testUserAccessory_NestedObjectConsistency() {
        val accessory = Accessory(404L, "Scarf", "url_scarf", 30)
        val userAccessory = UserAccessory(5L, false, accessory)

        assertTrue(userAccessory.accessories === accessory)
    }
}