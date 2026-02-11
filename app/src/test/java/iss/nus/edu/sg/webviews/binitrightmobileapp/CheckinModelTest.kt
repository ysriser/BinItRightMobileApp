package iss.nus.edu.sg.webviews.binitrightmobileapp

import iss.nus.edu.sg.webviews.binitrightmobileapp.model.CheckInData
import org.junit.Assert.assertEquals
import org.junit.Test

class CheckinModelTest {

    @Test
    fun checkInData_holdsAllValuesCorrectly() {
        val data = CheckInData(
            duration = 12,
            binId = "BIN123",
            wasteCategory = "Plastic",
            quantity = 5,
            videoKey = "video.mp4",
            checkInTime = "2026-01-10T10:00:00"
        )

        assertEquals(12, data.duration)
        assertEquals("BIN123", data.binId)
        assertEquals("Plastic", data.wasteCategory)
        assertEquals(5, data.quantity)
        assertEquals("video.mp4", data.videoKey)
        assertEquals("2026-01-10T10:00:00", data.checkInTime)
    }
}