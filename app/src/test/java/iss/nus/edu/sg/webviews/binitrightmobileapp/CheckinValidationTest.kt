package iss.nus.edu.sg.webviews.binitrightmobileapp

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CheckinValidationTest {

    @Test
    fun quantityAbove10_requiresVideo() {
        val quantity = 12
        val videoFile: File? = null

        val requiresVideo = quantity > 10 && videoFile == null

        assertTrue(requiresVideo)
    }

    @Test
    fun quantityBelow10_doesNotRequireVideo() {
        val quantity = 5
        val videoFile: File? = null

        val requiresVideo = quantity > 10 && videoFile == null

        assertFalse(requiresVideo)
    }

    @Test
    fun videoDuration_above5Seconds_isValid() {
        val durationSeconds = 8

        assertTrue(durationSeconds > 5)
    }

    @Test
    fun videoDuration_below5Seconds_isInvalid() {
        val durationSeconds = 3

        assertFalse(durationSeconds > 5)
    }

}