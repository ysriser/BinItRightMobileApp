package iss.nus.edu.sg.webviews.binitrightmobileapp

import iss.nus.edu.sg.webviews.binitrightmobileapp.model.DropOffLocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DropOffLocationModelTest {

    @Test
    fun dropOffLocation_holdsAllValuesCorrectly() {
        val location = DropOffLocation(
            id = "BIN123",
            name = "Central Mall Bin",
            address = "123 Orchard Road",
            postalCode = "238888",
            description = "Blue recycling bin",
            binType = "BLUEBIN",
            latitude = 1.304,
            longitude = 103.831,
            status = true,
            distanceMeters = 250.5
        )

        assertEquals("BIN123", location.id)
        assertEquals("Central Mall Bin", location.name)
        assertEquals("123 Orchard Road", location.address)
        assertEquals("238888", location.postalCode)
        assertEquals("Blue recycling bin", location.description)
        assertEquals("BLUEBIN", location.binType)
        assertEquals(1.304, location.latitude, 0.0)
        assertEquals(103.831, location.longitude, 0.0)
        assertTrue(location.status)
        assertEquals(250.5, location.distanceMeters, 0.0)
    }

    @Test
    fun dropOffLocation_equalityWorksCorrectly() {
        val location1 = createSample()
        val location2 = createSample()

        assertEquals(location1, location2)
        assertEquals(location1.hashCode(), location2.hashCode())
    }

    @Test
    fun dropOffLocation_copyCreatesModifiedObject() {
        val original = createSample()
        val updated = original.copy(distanceMeters = 100.0)

        assertEquals("BIN123", updated.id)
        assertEquals(100.0, updated.distanceMeters, 0.0)
        assertNotEquals(original.distanceMeters, updated.distanceMeters)
    }

    @Test
    fun dropOffLocation_toString_containsFields() {
        val location = createSample()

        val str = location.toString()

        assertTrue(str.contains("BIN123"))
        assertTrue(str.contains("Central Mall Bin"))
    }

    @Test
    fun dropOffLocation_sortByDistanceAscending() {
        val bins = listOf(
            createSample().copy(distanceMeters = 500.0),
            createSample().copy(id = "BIN2", distanceMeters = 100.0)
        )

        val sorted = bins.sortedBy { it.distanceMeters }

        assertEquals("BIN2", sorted.first().id)
    }




    private fun createSample() = DropOffLocation(
        id = "BIN123",
        name = "Central Mall Bin",
        address = "123 Orchard Road",
        postalCode = "238888",
        description = "Blue recycling bin",
        binType = "BLUEBIN",
        latitude = 1.304,
        longitude = 103.831,
        status = true,
        distanceMeters = 250.5
    )
}