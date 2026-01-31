package iss.nus.edu.sg.webviews.binitrightmobileapp

/*
 * File purpose:
 * - Basic tests for Check-In distance validation.
 * - Ensures LocationChecker accepts close points and rejects far points.
 */

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationCheckerTest {

    @Test
    fun isWithinRadius_returnsTrueForSamePoint() {
        // Step 1: use the same coordinates for user and bin.
        val result = LocationChecker.isWithinRadius(
            userLat = 1.3000,
            userLng = 103.8000,
            binLat = 1.3000,
            binLng = 103.8000,
            radius = 10.0
        )

        // Step 2: distance should be zero, so result is true.
        assertTrue(result)
    }

    @Test
    fun isWithinRadius_returnsFalseForFarPoint() {
        // Step 1: use two points that are far apart.
        val result = LocationChecker.isWithinRadius(
            userLat = 1.3000,
            userLng = 103.8000,
            binLat = 1.4000,
            binLng = 103.9000,
            radius = 1000.0 // 1km
        )

        // Step 2: distance should be much larger than 1km.
        assertFalse(result)
    }
}
