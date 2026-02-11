package iss.nus.edu.sg.webviews.binitrightmobileapp

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanResponseParsingTest {

    private val gson = Gson()

    @Test
    fun parse_validScanResponse_mapsCorrectly() {

        val json = """
        {
          "status": "SUCCESS",
          "data": {
            "final": {
              "category": "plastic",
              "recyclable": true,
              "confidence": 0.87,
              "instruction": "Recycle in blue bin"
            }
          }
        }
        """.trimIndent()

        val result = gson.fromJson(json, ScanResponse::class.java)

        assertEquals("SUCCESS", result.status)
        assertEquals("plastic", result.data?.final?.category)
        assertTrue(result.data?.final?.recyclable == true)
    }
}