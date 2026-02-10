package iss.nus.edu.sg.webviews.binitrightmobileapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanResultModelTest {

    @Test
    fun scanResult_usesExpectedDefaultsWhenOptionalFieldsMissing() {
        val result = ScanResult(
            category = "Paper cup",
            recyclable = false,
            confidence = 0.42f,
        )

        assertEquals("Paper cup", result.category)
        assertEquals(false, result.recyclable)
        assertEquals(0.42f, result.confidence)
        assertTrue(result.instructions.isEmpty())
        assertEquals(null, result.instruction)
        assertEquals(null, result.binType)
        assertEquals(null, result.debugMessage)
    }

    @Test
    fun feedbackRequest_holdsRequiredFields() {
        val feedback = FeedbackRequest(
            imageId = "img-1",
            userFeedback = true,
            timestamp = 123L,
        )

        assertEquals("img-1", feedback.imageId)
        assertEquals(true, feedback.userFeedback)
        assertEquals(123L, feedback.timestamp)
    }

    @Test
    fun tier1Result_holdsTop3Payload() {
        val top3 = listOf(
            mapOf("label" to "plastic", "p" to 0.8f),
            mapOf("label" to "metal", "p" to 0.1f),
            mapOf("label" to "glass", "p" to 0.05f),
        )

        val tier1 = Tier1Result(
            category = "plastic",
            confidence = 0.8f,
            top3 = top3,
            escalate = false,
        )

        assertEquals("plastic", tier1.category)
        assertEquals(0.8f, tier1.confidence)
        assertEquals(3, tier1.top3.size)
        assertEquals(false, tier1.escalate)
    }
}
