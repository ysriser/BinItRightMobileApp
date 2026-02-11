package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.app.Application
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import iss.nus.edu.sg.webviews.binitrightmobileapp.network.RetrofitClient
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response
import java.io.File
import java.lang.reflect.Proxy

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ScanRepositoryBranchTest {

    private val scanRepositoryKtClass =
        Class.forName("iss.nus.edu.sg.webviews.binitrightmobileapp.ScanRepositoryKt")

    @Test
    fun buildTier2DebugMessage_andCompressHelpers_coverNullAndMissingFileBranches() {
        val nullMetaMessage = invokePrivateStatic("buildTier2DebugMessage", null) as String?
        assertNull(nullMetaMessage)

        val unknownMetaMessage = invokePrivateStatic(
            "buildTier2DebugMessage",
            Meta(tier2ProviderAttempted = "mock", tier2ProviderUsed = "mock")
        ) as String?
        assertNull(unknownMetaMessage)

        val missingFile = File("Z:/definitely_missing_file.jpg")
        val compressed = invokePrivateStatic("compressImageForUpload", missingFile) as ByteArray
        assertEquals(0, compressed.size)
    }

    @Test
    fun realRepository_scanImage_success_andExplicitFailures() = runTest {
        val image = File.createTempFile("scan-branch-", ".jpg")
        image.writeBytes(ByteArray(2048) { (it % 255).toByte() })

        installApiServiceStub { methodName, _ ->
            when (methodName) {
                "scanImage" -> Response.success(
                    ScanResponse(
                        status = "success",
                        data = ScanResponseData(
                            final = FinalResult(
                                category = "Plastic bottle",
                                recyclable = true,
                                confidence = 0.91f,
                                instruction = "Rinse and recycle",
                                instructions = listOf("Rinse", "Dry")
                            ),
                            meta = Meta(
                                tier2ProviderAttempted = "openai",
                                tier2ProviderUsed = "openai"
                            )
                        )
                    )
                )
                else -> throw UnsupportedOperationException(methodName)
            }
        }

        val repo = RealScanRepository()
        val success = repo.scanImage(image)
        assertTrue(success.isSuccess)
        assertEquals("Plastic bottle", success.getOrNull()?.category)
        assertEquals("Tier2 provider: openai", success.getOrNull()?.debugMessage)

        installApiServiceStub { methodName, _ ->
            when (methodName) {
                "scanImage" -> Response.error<ScanResponse>(
                    413,
                    "too large".toResponseBody("text/plain".toMediaType())
                )
                else -> throw UnsupportedOperationException(methodName)
            }
        }
        val tooLarge = repo.scanImage(image)
        assertTrue(tooLarge.isFailure)
        assertTrue(tooLarge.exceptionOrNull()?.message.orEmpty().contains("413"))

        installApiServiceStub { methodName, _ ->
            when (methodName) {
                "scanImage" -> Response.success(
                    ScanResponse(status = "error", message = "bad response", data = ScanResponseData())
                )
                else -> throw UnsupportedOperationException(methodName)
            }
        }
        val noFinal = repo.scanImage(image)
        assertTrue(noFinal.isFailure)
        assertTrue(noFinal.exceptionOrNull()?.message.orEmpty().contains("no final data"))

        image.delete()
    }

    @Test
    fun realRepository_sendFeedback_unsuccessfulResponse_returnsFailure() = runTest {
        installApiServiceStub { methodName, _ ->
            when (methodName) {
                "sendFeedback" -> Response.error<Boolean>(
                    500,
                    "feedback failed".toResponseBody("text/plain".toMediaType())
                )
                else -> throw UnsupportedOperationException(methodName)
            }
        }

        val repo = RealScanRepository()
        val result = repo.sendFeedback(
            FeedbackRequest(
                imageId = "img-9",
                userFeedback = false,
                timestamp = 777L
            )
        )

        assertFalse(result.isSuccess)
        assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("Feedback error"))
    }

    @Test
    fun hybridAndLocalRepository_helpers_coverAdditionalMappingBranches() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val hybrid = HybridScanRepository(app)
        val local = LocalModelScanRepository(app)

        val mappedFinal = invokePrivateInstance(
            hybrid,
            "mapFinalToScanResult",
            FinalResult(
                category = "food waste",
                recyclable = false,
                confidence = 0.3f,
                instruction = "general waste",
                instructions = emptyList()
            ),
            null
        ) as ScanResult
        assertFalse(mappedFinal.recyclable)
        assertEquals("", mappedFinal.binType)
        assertTrue(mappedFinal.instructions.isNotEmpty())

        val mappedTier1 = invokePrivateInstance(
            hybrid,
            "mapTier1ToScanResult",
            Tier1Result(
                category = "food waste",
                confidence = 0.4f,
                top3 = emptyList(),
                escalate = false
            )
        ) as ScanResult
        assertEquals("Food waste", mappedTier1.category)
        assertFalse(mappedTier1.recyclable)
        assertEquals("", mappedTier1.binType)

        val hybridFeedback = hybrid.sendFeedback(
            FeedbackRequest("x", true, 1L)
        )
        val localFeedback = local.sendFeedback(
            FeedbackRequest("y", false, 2L)
        )
        assertTrue(hybridFeedback.isSuccess)
        assertTrue(localFeedback.isSuccess)
    }

    @Test
    fun downscaleLongSide_coversResizeAndNoResizePaths() {
        val small = Bitmap.createBitmap(200, 150, Bitmap.Config.ARGB_8888)
        val unchanged = invokePrivateStatic("downscaleLongSide", small, 960) as Bitmap
        assertTrue(unchanged === small)

        val large = Bitmap.createBitmap(2000, 1000, Bitmap.Config.ARGB_8888)
        val scaled = invokePrivateStatic("downscaleLongSide", large, 960) as Bitmap
        assertTrue(scaled.width <= 960)
        assertTrue(scaled.height <= 960)
        assertFalse(scaled === large)

        small.recycle()
        if (!large.isRecycled) {
            large.recycle()
        }
        if (!scaled.isRecycled) {
            scaled.recycle()
        }
    }

    private fun invokePrivateStatic(methodName: String, vararg args: Any?): Any? {
        val method = scanRepositoryKtClass.declaredMethods.first {
            it.name == methodName && it.parameterTypes.size == args.size
        }
        method.isAccessible = true
        return method.invoke(null, *args)
    }

    private fun installApiServiceStub(handler: (methodName: String, args: Array<Any?>?) -> Any?) {
        val proxy = Proxy.newProxyInstance(
            ApiService::class.java.classLoader,
            arrayOf(ApiService::class.java)
        ) { _, method, args ->
            when (method.name) {
                "toString" -> "ApiServiceStub"
                "hashCode" -> 0
                "equals" -> false
                else -> handler(method.name, args)
            }
        } as ApiService

        val apiField = RetrofitClient::class.java.getDeclaredField("api")
        apiField.isAccessible = true
        apiField.set(RetrofitClient, proxy)
    }

    private fun invokePrivateInstance(instance: Any, methodName: String, vararg args: Any?): Any? {
        val method = instance.javaClass.declaredMethods.first {
            it.name == methodName && it.parameterTypes.size == args.size
        }
        method.isAccessible = true
        return method.invoke(instance, *args)
    }
}
