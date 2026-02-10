package iss.nus.edu.sg.webviews.binitrightmobileapp

import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ScanRepositoryFallbackTest {

    @Test
    fun fakeRepository_returnsMockResult() = runTest {
        val repo = FakeScanRepository()
        val tempFile = File.createTempFile("fake-scan", ".jpg")
        tempFile.writeBytes(byteArrayOf(1, 2, 3))

        val result = repo.scanImage(tempFile)

        assertTrue(result.isSuccess)
        val payload = result.getOrNull() ?: error("Expected success payload")
        assertEquals("Electronics", payload.category)
        assertTrue(payload.instructions.isNotEmpty())

        tempFile.delete()
    }

    @Test
    fun realRepository_whenRetrofitUnavailable_fallsBackToFakeResult() = runTest {
        val repo = RealScanRepository()
        val tempFile = File.createTempFile("real-scan", ".jpg")
        tempFile.writeBytes(byteArrayOf(10, 20, 30, 40))

        val result = repo.scanImage(tempFile, forceTier2 = true)

        assertTrue(result.isSuccess)
        val payload = result.getOrNull() ?: error("Expected fallback payload")
        assertEquals("Electronics", payload.category)
        assertEquals(true, payload.recyclable)

        tempFile.delete()
    }

    @Test
    fun realRepository_sendFeedback_whenRetrofitUnavailable_returnsSuccess() = runTest {
        val repo = RealScanRepository()
        val feedback = FeedbackRequest(
            imageId = "img-1",
            userFeedback = true,
            timestamp = 123L,
        )

        val result = repo.sendFeedback(feedback)

        assertTrue(result.isSuccess)
        assertEquals(true, result.getOrNull())
    }
}
