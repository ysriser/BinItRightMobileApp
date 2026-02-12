package iss.nus.edu.sg.webviews.binitrightmobileapp

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanApiContractModelTest {

    private val gson = Gson()

    @Test
    fun scanResponse_parsesFinalFiveFieldsAndMeta() {
        val json = """
            {
              "status": "success",
              "request_id": "req-1",
              "data": {
                "decision": {
                  "used_tier2": true,
                  "reason_codes": ["FORCE_CLOUD"]
                },
                "final": {
                  "category": "E-waste - Phone Charger",
                  "recyclable": false,
                  "confidence": 0.91,
                  "instruction": "Bring this item to an e-waste collection point.",
                  "instructions": [
                    "Remove detachable battery if safe.",
                    "Keep terminals covered.",
                    "Drop at e-waste collection point."
                  ]
                },
                "meta": {
                  "schema_version": "0.1",
                  "force_cloud": true,
                  "tier2_provider_attempted": "openai",
                  "tier2_provider_used": "openai"
                }
              }
            }
        """.trimIndent()

        val response = gson.fromJson(json, ScanResponse::class.java)

        assertEquals("success", response.status)
        assertEquals("req-1", response.requestId)

        val final = response.data?.final
        assertNotNull(final)
        assertEquals("E-waste - Phone Charger", final?.category)
        assertFalse(final?.recyclable ?: true)
        assertTrue((final?.instructions?.size ?: 0) >= 3)

        val decision = response.data?.decision
        assertEquals(true, decision?.usedTier2)
        assertEquals("FORCE_CLOUD", decision?.reasonCodes?.first())

        val meta = response.data?.meta
        assertEquals("0.1", meta?.schemaVersion)
        assertEquals("openai", meta?.tier2ProviderUsed)
    }

    @Test
    fun scanResponse_parsesTier2ErrorObject() {
        val json = """
            {
              "status": "success",
              "data": {
                "final": {
                  "category": "Uncertain",
                  "recyclable": false,
                  "confidence": 0.5,
                  "instruction": "Dispose as general waste.",
                  "instructions": ["Dispose as general waste."]
                },
                "meta": {
                  "tier2_provider_attempted": "openai",
                  "tier2_provider_used": "mock",
                  "tier2_error": {
                    "http_status": "429",
                    "code": "rate_limit",
                    "message": "Too many requests"
                  }
                }
              }
            }
        """.trimIndent()

        val response = gson.fromJson(json, ScanResponse::class.java)
        val error = response.data?.meta?.tier2Error

        assertNotNull(error)
        assertEquals("429", error?.httpStatus)
        assertEquals("rate_limit", error?.code)
        assertEquals("Too many requests", error?.message)
    }

    @Test
    fun decision_defaultsReasonCodesToEmptyList() {
        val decision = Decision(usedTier2 = false)

        assertFalse(decision.usedTier2)
        assertTrue(decision.reasonCodes.isEmpty())
    }
}
