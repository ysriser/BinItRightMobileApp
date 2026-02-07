package iss.nus.edu.sg.webviews.binitrightmobileapp

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanApiContractTest {

    private val gson = Gson()

    @Test
    fun parseV01Response_keepsFinalFiveFields() {
        val json = """
            {
              "status": "success",
              "request_id": "req-123",
              "data": {
                "tier1": {
                  "category": "plastic",
                  "confidence": 0.91,
                  "escalate": false,
                  "top3": [
                    {"label": "plastic", "p": 0.91},
                    {"label": "glass", "p": 0.06},
                    {"label": "paper", "p": 0.03}
                  ]
                },
                "decision": {
                  "used_tier2": true,
                  "reason_codes": ["FORCE_CLOUD"]
                },
                "final": {
                  "category": "Plastic takeaway cup",
                  "recyclable": true,
                  "confidence": 0.88,
                  "instruction": "Empty and rinse before recycling.",
                  "instructions": [
                    "Empty all liquid.",
                    "Rinse the cup and lid.",
                    "Recycle clean plastic in the blue bin."
                  ]
                },
                "meta": {
                  "schema_version": "0.1",
                  "tier2_provider_used": "openai"
                }
              }
            }
        """.trimIndent()

        val parsed = gson.fromJson(json, ScanResponse::class.java)

        assertEquals("success", parsed.status)
        assertNotNull(parsed.data)
        assertEquals("Plastic takeaway cup", parsed.data?.final?.category)
        assertTrue(parsed.data?.final?.recyclable == true)
        assertTrue((parsed.data?.final?.instructions?.size ?: 0) >= 2)
        assertEquals("openai", parsed.data?.meta?.tier2_provider_used)
    }

    @Test
    fun parseTier1ReasonCodes_andCategoryMapping_workTogether() {
        val json = """
            {
              "status": "success",
              "data": {
                "tier1": {
                  "category": "E-waste - Phone Charger",
                  "confidence": 0.77,
                  "escalate": true,
                  "top3": [
                    {"label": "e-waste", "p": 0.77},
                    {"label": "plastic", "p": 0.18},
                    {"label": "metal", "p": 0.05}
                  ]
                },
                "decision": {
                  "used_tier2": true,
                  "reason_codes": ["TIER1_ESCALATE", "LOW_CONFIDENCE"]
                },
                "final": {
                  "category": "E-waste - Phone Charger",
                  "recyclable": false,
                  "confidence": 0.84,
                  "instruction": "Bring to an e-waste collection point.",
                  "instructions": [
                    "Do not place this in blue bins.",
                    "Remove detachable battery if safe.",
                    "Bring to an e-waste collection point."
                  ]
                }
              }
            }
        """.trimIndent()

        val parsed = gson.fromJson(json, ScanResponse::class.java)
        val decision = parsed.data?.decision

        assertNotNull(decision)
        assertTrue(decision?.used_tier2 == true)
        assertTrue(decision?.reason_codes?.contains("TIER1_ESCALATE") == true)

        val wasteType = WasteCategoryMapper.mapCategoryToWasteType(parsed.data?.final?.category)
        assertEquals(WasteCategoryMapper.TYPE_EWASTE, wasteType)
        assertEquals("EWASTE", WasteCategoryMapper.mapWasteTypeToBinType(wasteType))
        assertFalse(parsed.data?.final?.recyclable == true)
    }
}