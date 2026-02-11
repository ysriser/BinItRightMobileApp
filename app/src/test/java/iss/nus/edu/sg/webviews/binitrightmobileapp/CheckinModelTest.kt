package iss.nus.edu.sg.webviews.binitrightmobileapp

import iss.nus.edu.sg.webviews.binitrightmobileapp.model.CheckInData
import org.junit.Assert.assertEquals
import org.junit.Test

class CheckinModelTest {

    @Test
    fun checkInData_holdsAllValuesCorrectly() {
        val objectRef = buildObjectRef()
        val data = CheckInData(12, "BIN123", "Plastic", 5, objectRef, "2026-01-10T10:00:00")

        assertEquals(12, data.duration)
        assertEquals("BIN123", data.binId)
        assertEquals("Plastic", data.wasteCategory)
        assertEquals(5, data.quantity)
        assertEquals(objectRef, data.videoKey)
        assertEquals("2026-01-10T10:00:00", data.checkInTime)
    }

    private fun buildObjectRef(): String = listOf("clip", "bin").joinToString(".")
}
