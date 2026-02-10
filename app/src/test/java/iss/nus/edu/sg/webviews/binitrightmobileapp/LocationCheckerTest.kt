package iss.nus.edu.sg.webviews.binitrightmobileapp

/*
 * File purpose:
 * - Basic tests for Check-In distance validation.
 * - Ensures LocationChecker accepts close points and rejects far points.
 */

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class LocationCheckerTest {

    @Test
    fun isWithinRadius_returnsTrueForSamePoint() {
        val result = LocationChecker.isWithinRadius(
            userLat = 1.3000,
            userLng = 103.8000,
            binLat = 1.3000,
            binLng = 103.8000,
            radius = 10.0
        )

        assertTrue(result)
    }

    @Test
    fun isWithinRadius_returnsFalseForFarPoint() {
        val result = LocationChecker.isWithinRadius(
            userLat = 1.3000,
            userLng = 103.8000,
            binLat = 1.4000,
            binLng = 103.9000,
            radius = 1000.0
        )

        assertFalse(result)
    }

    @Test
    fun isWithinRadius_boundaryDistance_isAccepted() {
        val result = LocationChecker.isWithinRadius(
            userLat = 1.3000,
            userLng = 103.8000,
            binLat = 1.3000,
            binLng = 103.8000,
            radius = 0.0
        )

        assertTrue(result)
    }
}
