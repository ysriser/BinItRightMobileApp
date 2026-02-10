package iss.nus.edu.sg.webviews.binitrightmobileapp

import java.io.File
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class VideoUploaderTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun uploadVideoToSpaces_returnsTrueAndReportsProgress_onSuccess() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("ok"))
        val tempFile = createTempVideoFile()
        val progress = mutableListOf<Int>()

        val result = VideoUploader.uploadVideoToSpaces(
            file = tempFile,
            presignedUrl = server.url("/upload").toString(),
            onProgress = { progress.add(it) },
        )

        assertTrue(result)
        assertTrue(progress.contains(100))

        val request = server.takeRequest()
        assertEquals("PUT", request.method)
        assertEquals("video/mp4", request.getHeader("Content-Type"))

        tempFile.delete()
    }

    @Test
    fun uploadVideoToSpaces_returnsFalse_onServerFailure() = runTest {
        server.enqueue(MockResponse().setResponseCode(500).setBody("error"))
        val tempFile = createTempVideoFile()
        val progress = mutableListOf<Int>()

        val result = VideoUploader.uploadVideoToSpaces(
            file = tempFile,
            presignedUrl = server.url("/upload").toString(),
            onProgress = { progress.add(it) },
        )

        assertFalse(result)
        assertFalse(progress.contains(100))

        tempFile.delete()
    }

    @Test
    fun uploadVideoToSpaces_returnsFalse_onInvalidUrl() = runTest {
        val tempFile = createTempVideoFile()

        val result = VideoUploader.uploadVideoToSpaces(
            file = tempFile,
            presignedUrl = "ht!tp://invalid-url",
        )

        assertFalse(result)
        tempFile.delete()
    }

    private fun createTempVideoFile(): File {
        val file = File.createTempFile("upload-test-", ".mp4")
        // Small but valid byte payload for upload path testing.
        file.writeBytes(ByteArray(8 * 1024) { (it % 127).toByte() })
        return file
    }
}
