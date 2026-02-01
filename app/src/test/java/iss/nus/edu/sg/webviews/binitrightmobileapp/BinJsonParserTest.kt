package iss.nus.edu.sg.webviews.binitrightmobileapp

/*
 * File purpose:
 * - Unit tests for BinJsonParser used by "Find Nearby Bins".
 * - Ensures JSON is converted to DropOffLocation list correctly.
 */

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class BinJsonParserTest {

    @Test
    fun parse_returnsListWithExpectedFields() {
        // Step 1: create a small JSON array with 1 bin.
        val json = """
            [
              {
                "id": 1,
                "name": "Bin A",
                "address": "123 Road",
                "description": "Plastic only",
                "postalCode": "111111",
                "binType": "BLUEBIN",
                "status": true,
                "latitude": 1.3,
                "longitude": 103.8,
                "distanceMeters": 12.5
              }
            ]
        """.trimIndent()

        // Step 2: parse JSON.
        val list = BinJsonParser.parse(json)

        // Step 3: verify content.
        assertEquals(1, list.size)
        val bin = list.first()
        assertEquals(1L, bin.id)
        assertEquals("Bin A", bin.name)
        assertEquals("123 Road", bin.address)
        assertEquals("Plastic only", bin.description)
        assertEquals("111111", bin.postalCode)
        assertEquals("BLUEBIN", bin.binType)
        assertEquals(true, bin.status)
        assertEquals(1.3, bin.latitude, 0.0001)
        assertEquals(103.8, bin.longitude, 0.0001)
        assertEquals(12.5, bin.distanceMeters, 0.0001)
    }

    @Test
    fun parse_allowsMissingDistanceMeters() {
        // Step 1: create JSON without distanceMeters.
        val json = """
            [
              {
                "id": 2,
                "name": "Bin B",
                "address": "456 Street",
                "description": "E-waste",
                "postalCode": "222222",
                "binType": "EWASTE",
                "status": false,
                "latitude": 1.31,
                "longitude": 103.81
              }
            ]
        """.trimIndent()

        // Step 2: parse JSON.
        val list = BinJsonParser.parse(json)

        // Step 3: distanceMeters should default to 0.0.
        assertNotNull(list.first())
        assertEquals(0.0, list.first().distanceMeters, 0.0001)
    }
}
