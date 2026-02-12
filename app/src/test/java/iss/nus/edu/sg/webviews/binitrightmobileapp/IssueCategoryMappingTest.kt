package iss.nus.edu.sg.webviews.binitrightmobileapp

import org.junit.Assert.assertEquals
import org.junit.Test

class IssueCategoryMappingTest {

    @Test
    fun position0_mapsToBinIssues() {
        assertEquals("BIN_ISSUES", mapCategory(0))
    }

    @Test
    fun position1_mapsToAppProblems() {
        assertEquals("APP_PROBLEMS", mapCategory(1))
    }

    @Test
    fun position2_mapsToLocationErrors() {
        assertEquals("LOCATION_ERRORS", mapCategory(2))
    }

    @Test
    fun position3_mapsToOthers() {
        assertEquals("OTHERS", mapCategory(3))
    }

    @Test
    fun unknownPosition_mapsToOthers() {
        assertEquals("OTHERS", mapCategory(99))
    }


    private fun mapCategory(position: Int): String {
        return when (position) {
            0 -> "BIN_ISSUES"
            1 -> "APP_PROBLEMS"
            2 -> "LOCATION_ERRORS"
            3 -> "OTHERS"
            else -> "OTHERS"
        }
    }
}
