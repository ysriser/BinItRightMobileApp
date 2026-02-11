package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.io.FileOutputStream
import okhttp3.MultipartBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ScanRepositoryInternalTest {

    private val scanRepositoryKtClass =
        Class.forName("iss.nus.edu.sg.webviews.binitrightmobileapp.ScanRepositoryKt")

    @Test
    fun buildTier2DebugMessage_handlesOpenAiAndFallbackCases() {
        val openAiMeta = Meta(
            tier2_provider_attempted = "openai",
            tier2_provider_used = "openai",
        )
        val openAiMsg = invokePrivateStatic("buildTier2DebugMessage", openAiMeta) as String?
        assertEquals("Tier2 provider: openai", openAiMsg)

        val fallbackMeta = Meta(
            tier2_provider_attempted = "openai",
            tier2_provider_used = "mock",
            tier2_error = Tier2Error(code = "timeout")
        )
        val fallbackMsg = invokePrivateStatic("buildTier2DebugMessage", fallbackMeta) as String?
        assertEquals("Tier2 fallback to mock (timeout)", fallbackMsg)
    }

    @Test
    fun createImageUploadPart_buildsMultipartWithExpectedMetadata() {
        val imageFile = createLargeJpegFile()

        val part = invokePrivateStatic("createImageUploadPart", imageFile) as MultipartBody.Part

        val disposition = part.headers?.get("Content-Disposition").orEmpty()
        assertTrue(disposition.contains("name=\"image\""))
        assertTrue(disposition.contains("_upload.jpg"))

        imageFile.delete()
    }

    @Test
    fun compressImageForUpload_invalidImage_returnsOriginalBytes() {
        val file = File.createTempFile("invalid-image-", ".jpg")
        val raw = ByteArray(420 * 1024) { (it % 251).toByte() }
        file.writeBytes(raw)

        val compressed = invokePrivateStatic("compressImageForUpload", file) as ByteArray

        assertTrue(compressed.isNotEmpty())
        assertTrue(compressed.size <= raw.size)
        file.delete()
    }

    @Test
    fun compressImageForUpload_validLargeImage_staysWithinUploadLimit() {
        val imageFile = createLargeJpegFile(width = 2600, height = 2600)

        val compressed = invokePrivateStatic("compressImageForUpload", imageFile) as ByteArray

        assertTrue(compressed.isNotEmpty())
        assertTrue(compressed.size <= 350 * 1024)
        imageFile.delete()
    }

    @Test
    fun realRepository_mapFinalToScanResult_appliesFallbackInstructionsAndDebugMessage() {
        val repository = RealScanRepository()
        val final = FinalResult(
            category = "E-waste - Charger",
            recyclable = false,
            confidence = 0.45f,
            instruction = "Bring to e-waste point",
            instructions = emptyList(),
        )
        val meta = Meta(
            tier2_provider_attempted = "openai",
            tier2_provider_used = "mock",
            tier2_error = Tier2Error(code = "schema")
        )

        val mapped = invokePrivateInstance(
            repository,
            "mapFinalToScanResult",
            final,
            meta
        ) as ScanResult

        assertTrue(mapped.recyclable)
        assertEquals("EWASTE", mapped.binType)
        assertEquals("Tier2 fallback to mock (schema)", mapped.debugMessage)
        assertTrue(mapped.instructions.isNotEmpty())
    }

    @Test
    fun hybridRepository_mapTier1ToScanResult_normalizesCategoryAndCategoryId() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val repository = HybridScanRepository(app)

        val tier1 = Tier1Result(
            category = "plastic",
            confidence = 0.91f,
            top3 = emptyList<Map<String, Any>>(),
            escalate = false,
        )

        val mapped = invokePrivateInstance(
            repository,
            "mapTier1ToScanResult",
            tier1
        ) as ScanResult

        assertEquals("Plastic", mapped.category)
        assertEquals("tier1.plastic", mapped.categoryId)
        assertEquals("BLUEBIN", mapped.binType)
        assertNotNull(mapped.instruction)
    }

    private fun invokePrivateStatic(methodName: String, vararg args: Any): Any? {
        val method = scanRepositoryKtClass.declaredMethods.first {
            it.name == methodName && it.parameterTypes.size == args.size
        }
        method.isAccessible = true
        return method.invoke(null, *args)
    }

    private fun invokePrivateInstance(instance: Any, methodName: String, vararg args: Any?): Any? {
        val method = instance.javaClass.declaredMethods.first {
            it.name == methodName && it.parameterTypes.size == args.size
        }
        method.isAccessible = true
        return method.invoke(instance, *args)
    }

    private fun createLargeJpegFile(width: Int = 2200, height: Int = 2200): File {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()

        val stepX = (width / 20).coerceAtLeast(1)
        val stepY = (height / 20).coerceAtLeast(1)

        for (x in 0 until width step stepX) {
            for (y in 0 until height step stepY) {
                val red = ((x * 17 + y * 13) % 255)
                val green = ((x * 31 + y * 7) % 255)
                val blue = ((x * 11 + y * 29) % 255)
                paint.color = Color.rgb(red, green, blue)
                canvas.drawRect(
                    x.toFloat(),
                    y.toFloat(),
                    (x + stepX).toFloat(),
                    (y + stepY).toFloat(),
                    paint
                )
            }
        }

        val file = File.createTempFile("scan-repo-large-", ".jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        }
        bitmap.recycle()
        return file
    }
}
