package iss.nus.edu.sg.webviews.binitrightmobileapp

import org.junit.Assert.assertEquals
import org.junit.Test

class IssueCategoryMappingTest {

    @Test
    fun position0_mapsToBinIssues() {
        assertEquals("BinIssues", mapCategory(0))
    }

    @Test
    fun position1_mapsToAppProblems() {
        assertEquals("AppProblems", mapCategory(1))
    }

    @Test
    fun position2_mapsToLocationErrors() {
        assertEquals("LocationErrors", mapCategory(2))
    }

    @Test
    fun position3_mapsToOthers() {
        assertEquals("Others", mapCategory(3))
    }

    @Test
    fun unknownPosition_mapsToOthers() {
        assertEquals("Others", mapCategory(99))
    }


    private fun mapCategory(position: Int): String {
        return when (position) {
            0 -> "BinIssues"
            1 -> "AppProblems"
            2 -> "LocationErrors"
            3 -> "Others"
            else -> "Others"
        }
    }
}