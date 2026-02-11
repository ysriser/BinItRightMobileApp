package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.core.graphics.scale
import iss.nus.edu.sg.webviews.binitrightmobileapp.network.RetrofitClient
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.File

private const val MAX_UPLOAD_BYTES = 350 * 1024
private const val INITIAL_JPEG_QUALITY = 80
private const val MIN_JPEG_QUALITY = 40
private const val UPLOAD_MAX_LONG_SIDE = 960
private const val MIN_BITMAP_SIDE = 256
private const val CLEAN_DRY_INSTRUCTION = "Clean and dry the item."
private const val PROPER_STREAM_INSTRUCTION = "Place it in the proper recycling stream."
private const val RECYCLE_SUMMARY_INSTRUCTION = "Clean and dry the item, then recycle it."
private val RECYCLABLE_FALLBACK_INSTRUCTIONS = listOf(
    CLEAN_DRY_INSTRUCTION,
    PROPER_STREAM_INSTRUCTION,
)
private val RECYCLABLE_CATEGORY_KEYWORDS = setOf(
    "plastic",
    "metal",
    "glass",
    "paper",
    "e-waste",
    "textile",
    "lighting",
)

interface ScanRepository {
    suspend fun scanImage(
        imageFile: File,
        forceTier2: Boolean = false,
        onStatusUpdate: (String) -> Unit = {}
    ): Result<ScanResult>

    suspend fun sendFeedback(feedback: FeedbackRequest): Result<Boolean>
}

class FakeScanRepository : ScanRepository {
    override suspend fun scanImage(
        imageFile: File,
        forceTier2: Boolean,
        onStatusUpdate: (String) -> Unit
    ): Result<ScanResult> {
        delay(1500)
        return Result.success(
            ScanResult(
                category = "Electronics",
                recyclable = true,
                confidence = 0.86f,
                instruction = "Bring this item to an e-waste collection point.",
                instructions = listOf(
                    "Remove batteries if removable.",
                    "Pack sharp parts safely.",
                    "Bring to an e-waste collection point."
                ),
                binType = "EWASTE"
            )
        )
    }

    override suspend fun sendFeedback(feedback: FeedbackRequest): Result<Boolean> {
        delay(500)
        return Result.success(true)
    }
}

class RealScanRepository : ScanRepository {
    override suspend fun scanImage(
        imageFile: File,
        forceTier2: Boolean,
        onStatusUpdate: (String) -> Unit
    ): Result<ScanResult> {
        return try {
            val imagePart = createImageUploadPart(imageFile)
            val forceCloudPart = if (forceTier2) {
                "true".toRequestBody("text/plain".toMediaTypeOrNull())
            } else {
                null
            }

            val response = RetrofitClient.apiService().scanImage(
                image = imagePart,
                forceCloud = forceCloudPart
            )

            if (response.isSuccessful && response.body() != null) {
                val scanResponse = response.body()!!
                val final = scanResponse.data?.final
                if (scanResponse.status == "success" && final != null) {
                    Result.success(mapFinalToScanResult(final, scanResponse.data?.meta))
                } else {
                    Result.failure(Exception("Server returned no final data"))
                }
            } else {
                if (response.code() == 413) {
                    Result.failure(Exception("Image too large (413). Try scanning closer to the item."))
                } else {
                    Result.failure(Exception("Network error: ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            Log.e("RealScanRepository", "Failed to scan, falling back to mock", e)
            FakeScanRepository().scanImage(imageFile, forceTier2, onStatusUpdate)
        }
    }

    override suspend fun sendFeedback(feedback: FeedbackRequest): Result<Boolean> {
        return try {
            val response = RetrofitClient.apiService().sendFeedback(feedback)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Feedback error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("RealScanRepository", "Failed to send feedback", e)
            Result.success(true)
        }
    }

    private fun mapFinalToScanResult(final: FinalResult, meta: Meta?): ScanResult {
        val effectiveRecyclable = final.recyclable || ScannedCategoryHelper.isSpecialRecyclable(final.category)
        val finalInstructions = final.instructions.ifEmpty {
            fallbackInstructionsFor(effectiveRecyclable)
        }

        return ScanResult(
            category = final.category,
            recyclable = effectiveRecyclable,
            confidence = final.confidence,
            instruction = final.instruction,
            instructions = finalInstructions,
            binType = ScannedCategoryHelper.toBinType(final.category, effectiveRecyclable),
            debugMessage = buildTier2DebugMessage(meta)
        )
    }
}

class LocalModelScanRepository(private val context: Context) : ScanRepository {
    private val classifier by lazy {
        ImageClassifier(
            context = context,
            inputSize = Tier1PreprocessConfig.INPUT_SIZE,
            resizeScale = Tier1PreprocessConfig.RESIZE_SCALE
        )
    }

    override suspend fun scanImage(
        imageFile: File,
        forceTier2: Boolean,
        onStatusUpdate: (String) -> Unit
    ): Result<ScanResult> {
        return try {
            val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
            val result = classifier.classify(bitmap)

            val recyclable = isRecyclableCategory(result.category)

            val categoryName = result.category.replaceFirstChar { it.uppercase() }
            delay(1000)

            Result.success(
                ScanResult(
                    category = categoryName,
                    recyclable = recyclable,
                    confidence = result.confidence,
                    instructions = if (recyclable) {
                        RECYCLABLE_FALLBACK_INSTRUCTIONS
                    } else {
                        listOf("Do not put this into the blue recycling bin.")
                    },
                    instruction = if (recyclable) {
                        RECYCLE_SUMMARY_INSTRUCTION
                    } else {
                        "Dispose through general or special waste handling."
                    },
                    binType = ScannedCategoryHelper.toBinType(categoryName, recyclable)
                )
            )
        } catch (e: Exception) {
            Log.e("LocalModelRepository", "Error running local model", e)
            Result.failure(e)
        }
    }

    override suspend fun sendFeedback(feedback: FeedbackRequest): Result<Boolean> {
        return Result.success(true)
    }
}

class HybridScanRepository(
    private val context: Context
) : ScanRepository {
    private val classifier by lazy {
        ImageClassifier(
            context = context,
            inputSize = Tier1PreprocessConfig.INPUT_SIZE,
            resizeScale = Tier1PreprocessConfig.RESIZE_SCALE
        )
    }
    private val gson = com.google.gson.Gson()

    override suspend fun scanImage(
        imageFile: File,
        forceTier2: Boolean,
        onStatusUpdate: (String) -> Unit
    ): Result<ScanResult> {
        return try {
            onStatusUpdate("Identifying...")
            val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
            val tier1Result = classifier.classify(bitmap)

            Log.d("HybridScanRepository", "Tier1 result: $tier1Result")

            val shouldEscalate = tier1Result.escalate || forceTier2
            if (shouldEscalate) {
                onStatusUpdate("Analyzing...")
                callTier2(imageFile, tier1Result, forceTier2)
            } else {
                Result.success(mapTier1ToScanResult(tier1Result))
            }
        } catch (e: Exception) {
            Log.e("HybridScanRepository", "Error in hybrid scan", e)
            Result.failure(e)
        }
    }

    private suspend fun callTier2(
        imageFile: File,
        tier1Result: Tier1Result,
        forceTier2: Boolean
    ): Result<ScanResult> {
        return try {
            val imagePart = createImageUploadPart(imageFile)

            val tier1Json = gson.toJson(tier1Result)
            val tier1Body = tier1Json.toRequestBody("application/json".toMediaTypeOrNull())
            val forceCloudBody = if (forceTier2) {
                "true".toRequestBody("text/plain".toMediaTypeOrNull())
            } else {
                null
            }

            val response = RetrofitClient.apiService().scanImage(
                image = imagePart,
                tier1 = tier1Body,
                forceCloud = forceCloudBody
            )

            if (response.isSuccessful && response.body() != null) {
                val scanResponse = response.body()!!
                val final = scanResponse.data?.final
                if (scanResponse.status == "success" && final != null) {
                    Result.success(mapFinalToScanResult(final, scanResponse.data?.meta))
                } else {
                    Result.failure(Exception("Server returned error: ${scanResponse.message}"))
                }
            } else {
                if (response.code() == 413) {
                    Result.failure(Exception("Image too large (413). Try scanning closer to the item."))
                } else {
                    Result.failure(Exception("Network error: ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            Log.e("HybridScanRepository", "Tier2 failed, fallback to Tier1", e)
            Result.success(mapTier1ToScanResult(tier1Result))
        }
    }

    private fun mapFinalToScanResult(final: FinalResult, meta: Meta?): ScanResult {
        val effectiveRecyclable = final.recyclable || ScannedCategoryHelper.isSpecialRecyclable(final.category)
        val finalInstructions = final.instructions.ifEmpty {
            fallbackInstructionsFor(effectiveRecyclable)
        }

        return ScanResult(
            category = final.category,
            recyclable = effectiveRecyclable,
            confidence = final.confidence,
            instruction = final.instruction,
            instructions = finalInstructions,
            binType = ScannedCategoryHelper.toBinType(final.category, effectiveRecyclable),
            debugMessage = buildTier2DebugMessage(meta)
        )
    }

    private fun mapTier1ToScanResult(tier1: Tier1Result): ScanResult {
        val recyclable = isRecyclableCategory(tier1.category)

        val categoryName = tier1.category.replaceFirstChar { it.uppercase() }

        return ScanResult(
            category = categoryName,
            recyclable = recyclable,
            confidence = tier1.confidence,
            instruction = if (recyclable) {
                RECYCLE_SUMMARY_INSTRUCTION
            } else {
                "Dispose through general or special waste handling."
            },
            instructions = if (recyclable) {
                RECYCLABLE_FALLBACK_INSTRUCTIONS
            } else {
                listOf("Do not put this into the blue recycling bin.")
            },
            categoryId = "tier1.${tier1.category}",
            binType = ScannedCategoryHelper.toBinType(categoryName, recyclable)
        )
    }

    override suspend fun sendFeedback(feedback: FeedbackRequest): Result<Boolean> {
        return Result.success(true)
    }
}

private fun buildTier2DebugMessage(meta: Meta?): String? {
    if (meta == null) {
        return null
    }

    val attempted = meta.tier2ProviderAttempted?.trim()?.lowercase()
    val used = meta.tier2ProviderUsed?.trim()?.lowercase()

    if (attempted == "openai" && used == "openai") {
        return "Tier2 provider: openai"
    }

    if (attempted == "openai" && used == "mock") {
        val errorCode = meta.tier2Error?.code?.takeIf { it.isNotBlank() } ?: "unknown"
        return "Tier2 fallback to mock ($errorCode)"
    }

    return null
}

private fun createImageUploadPart(imageFile: File): MultipartBody.Part {
    val uploadBytes = compressImageForUpload(imageFile)
    val requestBody = uploadBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
    val uploadName = imageFile.nameWithoutExtension + "_upload.jpg"
    return MultipartBody.Part.createFormData("image", uploadName, requestBody)
}

private fun compressImageForUpload(imageFile: File): ByteArray {
    if (!imageFile.exists()) {
        return ByteArray(0)
    }

    val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
    if (bitmap == null) {
        return imageFile.readBytes()
    }

    val normalized = downscaleLongSide(bitmap, UPLOAD_MAX_LONG_SIDE)
    if (normalized !== bitmap) {
        bitmap.recycle()
    }

    val compressed = compressBitmap(normalized)
    normalized.recycle()
    return compressed
}

private fun downscaleLongSide(source: Bitmap, maxLongSide: Int): Bitmap {
    val longSide = maxOf(source.width, source.height)
    if (longSide <= maxLongSide) {
        return source
    }

    val scale = maxLongSide.toFloat() / longSide.toFloat()
    val newWidth = (source.width * scale).toInt().coerceAtLeast(MIN_BITMAP_SIDE)
    val newHeight = (source.height * scale).toInt().coerceAtLeast(MIN_BITMAP_SIDE)
    return source.scale(newWidth, newHeight, true)
}

private fun compressBitmap(source: Bitmap): ByteArray {
    var workingBitmap = source
    var quality = INITIAL_JPEG_QUALITY

    while (true) {
        val bytes = compressBitmapToJpegBytes(workingBitmap, quality)

        if (bytes.size <= MAX_UPLOAD_BYTES) {
            recycleIfTemporaryBitmap(source, workingBitmap)
            return bytes
        }

        if (quality > MIN_JPEG_QUALITY) {
            quality -= 8
            continue
        }

        val resized = downscaleForRetry(source, workingBitmap)
        if (resized == null) {
            recycleIfTemporaryBitmap(source, workingBitmap)
            return bytes
        }
        workingBitmap = resized
        quality = INITIAL_JPEG_QUALITY
    }
}

private fun isRecyclableCategory(category: String): Boolean {
    return category.lowercase() in RECYCLABLE_CATEGORY_KEYWORDS
}

private fun fallbackInstructionsFor(recyclable: Boolean): List<String> {
    return if (recyclable) {
        RECYCLABLE_FALLBACK_INSTRUCTIONS
    } else {
        listOf("Dispose in general waste unless official guidance says otherwise.")
    }
}

private fun compressBitmapToJpegBytes(bitmap: Bitmap, quality: Int): ByteArray {
    return ByteArrayOutputStream().use { stream ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        stream.toByteArray()
    }
}

private fun downscaleForRetry(source: Bitmap, bitmap: Bitmap): Bitmap? {
    val newWidth = (bitmap.width * 0.75f).toInt().coerceAtLeast(MIN_BITMAP_SIDE)
    val newHeight = (bitmap.height * 0.75f).toInt().coerceAtLeast(MIN_BITMAP_SIDE)
    if (newWidth == bitmap.width && newHeight == bitmap.height) {
        return null
    }

    val resized = bitmap.scale(newWidth, newHeight, true)
    recycleIfTemporaryBitmap(source, bitmap)
    return resized
}

private fun recycleIfTemporaryBitmap(source: Bitmap, bitmap: Bitmap) {
    if (bitmap !== source) {
        bitmap.recycle()
    }
}
