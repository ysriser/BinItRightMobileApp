package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.content.Context
import android.util.Log
import iss.nus.edu.sg.webviews.binitrightmobileapp.network.RetrofitClient
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

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
                recyclable = false,
                confidence = 0.86f,
                instruction = "Bring this item to an e-waste collection point.",
                instructions = listOf(
                    "Remove batteries if removable.",
                    "Pack sharp parts safely.",
                    "Bring to an e-waste collection point."
                ),
                binType = "EWaste"
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
            val requestFile = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
            val imagePart = MultipartBody.Part.createFormData("image", imageFile.name, requestFile)
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
                    Result.success(mapFinalToScanResult(final))
                } else {
                    Result.failure(Exception("Server returned no final data"))
                }
            } else {
                Result.failure(Exception("Network error: ${response.code()}"))
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

    private fun mapFinalToScanResult(final: FinalResult): ScanResult {
        val fallbackInstructions = if (final.recyclable) {
            listOf("Clean and dry the item.", "Place it in the blue recycling bin.")
        } else {
            listOf("Dispose in general waste unless official guidance says otherwise.")
        }

        val finalInstructions = if (final.instructions.isEmpty()) {
            fallbackInstructions
        } else {
            final.instructions
        }

        return ScanResult(
            category = final.category,
            recyclable = final.recyclable,
            confidence = final.confidence,
            instruction = final.instruction,
            instructions = finalInstructions,
            binType = determineBinType(final.category, final.recyclable)
        )
    }

    private fun determineBinType(category: String, recyclable: Boolean): String {
        return when {
            category.startsWith("E-waste - ", ignoreCase = true) -> "EWaste"
            category.startsWith("Textile - ", ignoreCase = true) -> "Textile"
            category.startsWith("Lighting - ", ignoreCase = true) -> "Lamp"
            recyclable -> "BlueBin"
            else -> "General"
        }
    }
}

class LocalModelScanRepository(private val context: Context) : ScanRepository {
    private val classifier by lazy { ImageClassifier(context) }

    override suspend fun scanImage(
        imageFile: File,
        forceTier2: Boolean,
        onStatusUpdate: (String) -> Unit
    ): Result<ScanResult> {
        return try {
            val bitmap = android.graphics.BitmapFactory.decodeFile(imageFile.absolutePath)
            val result = classifier.classify(bitmap)

            val isRecyclable = when (result.category.lowercase()) {
                "plastic", "metal", "glass", "paper" -> true
                else -> false
            }

            delay(1000)

            Result.success(
                ScanResult(
                    category = result.category.replaceFirstChar { it.uppercase() },
                    recyclable = isRecyclable,
                    confidence = result.confidence,
                    instructions = if (isRecyclable) {
                        listOf("Clean and dry the item.", "Place it in the blue recycling bin.")
                    } else {
                        listOf("Do not put this into the blue recycling bin.")
                    },
                    instruction = if (isRecyclable) {
                        "Clean and dry the item, then recycle it."
                    } else {
                        "Dispose through general or special waste handling."
                    }
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
    private val classifier by lazy { ImageClassifier(context) }
    private val gson = com.google.gson.Gson()

    override suspend fun scanImage(
        imageFile: File,
        forceTier2: Boolean,
        onStatusUpdate: (String) -> Unit
    ): Result<ScanResult> {
        return try {
            onStatusUpdate("Identifying...")
            val bitmap = android.graphics.BitmapFactory.decodeFile(imageFile.absolutePath)
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
            val requestFile = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
            val imagePart = MultipartBody.Part.createFormData("image", imageFile.name, requestFile)

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
                    Result.success(mapFinalToScanResult(final))
                } else {
                    Result.failure(Exception("Server returned error: ${scanResponse.message}"))
                }
            } else {
                Result.failure(Exception("Network error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("HybridScanRepository", "Tier2 failed, fallback to Tier1", e)
            Result.success(mapTier1ToScanResult(tier1Result))
        }
    }

    private fun mapFinalToScanResult(final: FinalResult): ScanResult {
        val fallbackInstructions = if (final.recyclable) {
            listOf("Clean and dry the item.", "Place it in the blue recycling bin.")
        } else {
            listOf("Dispose in general waste unless official guidance says otherwise.")
        }

        val finalInstructions = if (final.instructions.isEmpty()) {
            fallbackInstructions
        } else {
            final.instructions
        }

        return ScanResult(
            category = final.category,
            recyclable = final.recyclable,
            confidence = final.confidence,
            instruction = final.instruction,
            instructions = finalInstructions,
            binType = determineBinType(final.category, final.recyclable)
        )
    }

    private fun mapTier1ToScanResult(tier1: Tier1Result): ScanResult {
        val recyclable = when (tier1.category.lowercase()) {
            "plastic", "metal", "glass", "paper" -> true
            else -> false
        }

        val categoryName = tier1.category.replaceFirstChar { it.uppercase() }

        return ScanResult(
            category = categoryName,
            recyclable = recyclable,
            confidence = tier1.confidence,
            instruction = if (recyclable) {
                "Clean and dry the item, then recycle it."
            } else {
                "Dispose through general or special waste handling."
            },
            instructions = if (recyclable) {
                listOf("Clean and dry the item.", "Place it in the blue recycling bin.")
            } else {
                listOf("Do not put this into the blue recycling bin.")
            },
            categoryId = "tier1.${tier1.category}",
            binType = determineBinType(categoryName, recyclable)
        )
    }

    private fun determineBinType(category: String, recyclable: Boolean): String {
        return when {
            category.startsWith("E-waste - ", ignoreCase = true) -> "EWaste"
            category.startsWith("Textile - ", ignoreCase = true) -> "Textile"
            category.startsWith("Lighting - ", ignoreCase = true) -> "Lamp"
            recyclable -> "BlueBin"
            else -> "General"
        }
    }

    override suspend fun sendFeedback(feedback: FeedbackRequest): Result<Boolean> {
        return Result.success(true)
    }
}
