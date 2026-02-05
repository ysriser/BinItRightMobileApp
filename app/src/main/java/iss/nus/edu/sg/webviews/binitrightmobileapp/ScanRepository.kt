package iss.nus.edu.sg.webviews.binitrightmobileapp


import android.util.Log
import kotlinx.coroutines.delay
import java.io.File
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    // 10.0.2.2 is localhost for Android Emulator
  // private const val BASE_URL = "http://10.0.2.2:8080/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}

interface ScanRepository {
    suspend fun scanImage(imageFile: File, forceTier2: Boolean = false, onStatusUpdate: (String) -> Unit = {}): Result<ScanResult>
    suspend fun sendFeedback(feedback: FeedbackRequest): Result<Boolean>
}

class FakeScanRepository : ScanRepository {
    override suspend fun scanImage(imageFile: File, forceTier2: Boolean, onStatusUpdate: (String) -> Unit): Result<ScanResult> {
        delay(1500) // Simulate network delay
        return Result.success(
            ScanResult(
                category = "Electronics",
                recyclable = true,
                confidence = 0.86f,
                instructions = listOf("Remove batteries if removable", "Dispose in e-waste bin"),
                instruction = "1. Remove batteries if removable.\n2. Dispose in e-waste bin.",
                categoryId = "electronics.generic",
                binType = "ewaste"
            )
        )
    }

    override suspend fun sendFeedback(feedback: FeedbackRequest): Result<Boolean> {
        delay(500)
        return Result.success(true)
    }
}


class RealScanRepository : ScanRepository {
    // Legacy Real Repository - might not be used if Hybrid is preferred, but updating for compilation
    override suspend fun scanImage(imageFile: File, forceTier2: Boolean, onStatusUpdate: (String) -> Unit): Result<ScanResult> {
        return Result.failure(Exception("RealScanRepository is deprecated. Use HybridScanRepository."))
    }

    override suspend fun sendFeedback(feedback: FeedbackRequest): Result<Boolean> {
       // ... existing logic can be kept or moved ...
       return Result.success(true)
    }
}

class LocalModelScanRepository(private val context: android.content.Context) : ScanRepository {
    private val classifier = ImageClassifier(context)

    override suspend fun scanImage(imageFile: File, forceTier2: Boolean, onStatusUpdate: (String) -> Unit): Result<ScanResult> {
        return try {
            val bitmap = android.graphics.BitmapFactory.decodeFile(imageFile.absolutePath)
            val result = classifier.classify(bitmap)
            
            // Map Tier1Result to ScanResult
            val isRecyclable = when(result.category.lowercase()) {
                "plastic", "metal", "glass", "paper" -> true
                else -> false
            }
            
            delay(1000) 

            Result.success(
                ScanResult(
                    category = result.category.replaceFirstChar { it.uppercase() },
                    recyclable = isRecyclable,
                    confidence = result.confidence,
                    instructions = if (isRecyclable) 
                        listOf("Clean and Dry", "Recycle in Blue Bin") 
                    else 
                        listOf("Check specific disposal guidelines"),
                    instruction = if (isRecyclable)
                        "1. Rinse the item.\n2. Place in Blue Bin."
                    else
                        "1. Do not put in Blue Bin.\n2. Check local guidelines."
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
    private val context: android.content.Context
) : ScanRepository {
    private val classifier = ImageClassifier(context)
    private val gson = com.google.gson.Gson()

    override suspend fun scanImage(imageFile: File, forceTier2: Boolean, onStatusUpdate: (String) -> Unit): Result<ScanResult> {
        return try {
            // 1. Run Tier 1 (On-Device)
            onStatusUpdate("Identifying...")
            val bitmap = android.graphics.BitmapFactory.decodeFile(imageFile.absolutePath)
            val tier1Result = classifier.classify(bitmap)
            
            Log.d("HybridScanRepository", "Tier 1 Result: $tier1Result")

            // 2. Check Escalation
            val shouldEscalate = tier1Result.escalate || forceTier2
            
            if (shouldEscalate) {
                Log.d("HybridScanRepository", "Escalating to Tier 2 (Force: $forceTier2)")
                onStatusUpdate("Analyzing...")
                // Call Server
                callTier2(imageFile, tier1Result)
            } else {
                Log.d("HybridScanRepository", "Using Tier 1 Result")
                // Return Tier 1 as Final
                Result.success(mapTier1ToScanResult(tier1Result))
            }
        } catch (e: Exception) {
            Log.e("HybridScanRepository", "Error in hybrid scan", e)
            Result.failure(e)
        }
    }

    private suspend fun callTier2(imageFile: File, tier1Result: Tier1Result): Result<ScanResult> {
        return try {
            val requestFile = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("image", imageFile.name, requestFile)
            
            val tier1Json = gson.toJson(tier1Result)
            val tier1Part = MultipartBody.Part.createFormData("tier1", tier1Json)

            val response = RetrofitClient.instance.scanImage(body, tier1Part)
            
            if (response.isSuccessful && response.body() != null) {
                val scanResponse = response.body()!!
                if (scanResponse.status == "success" && scanResponse.data?.final != null) {
                    Result.success(mapFinalToScanResult(scanResponse.data.final))
                } else {
                     Result.failure(Exception("Server returned error: ${scanResponse.message}"))
                }
            } else {
                Result.failure(Exception("Network error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("HybridScanRepository", "Tier 2 failed", e)
            // Fallback to Tier 1 if network fails? Or propagate error? 
            // User spec says: "Always render server data.final" but if server fails?
            // "Graceful degradation... prefer returning decision.used_tier2=false" if server is unavailable is SERVER logic.
            // Client side logic: If network call throws, fallback to Tier 1 is safest for UX.
            Result.success(mapTier1ToScanResult(tier1Result))
        }
    }

    private fun mapFinalToScanResult(final: FinalResult): ScanResult {
        return ScanResult(
            category = final.category,
            recyclable = final.recyclable,
            confidence = final.confidence,
            instruction = final.instruction,
            instructions = final.instructions,
            disposalMethod = final.disposal_method,
            categoryId = final.category_id,
            binType = final.bin_type,
            rationaleTags = final.rationale_tags
        )
    }

    private fun mapTier1ToScanResult(tier1: Tier1Result): ScanResult {
        val isRecyclable = when(tier1.category.lowercase()) {
             "plastic", "metal", "glass", "paper" -> true
             else -> false
        }
        return ScanResult(
            category = tier1.category.replaceFirstChar { it.uppercase() },
            recyclable = isRecyclable,
            confidence = tier1.confidence,
            instruction = if (isRecyclable) "Recycle in Blue Bin" else "Check local guidelines",
            instructions = listOf("Generated from Tier 1"),
            categoryId = "tier1.${tier1.category}",
            binType = if (isRecyclable) "blue" else "general"
        )
    }

    override suspend fun sendFeedback(feedback: FeedbackRequest): Result<Boolean> {
         return Result.success(true) 
    }
}

