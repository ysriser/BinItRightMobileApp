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
    private const val BASE_URL = "http://192.168.88.4:8082/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}

interface ScanRepository {
    suspend fun scanImage(imageFile: File): Result<ScanResult>
    suspend fun sendFeedback(feedback: FeedbackRequest): Result<Boolean>
}

class FakeScanRepository : ScanRepository {
    override suspend fun scanImage(imageFile: File): Result<ScanResult> {
        delay(1500) // Simulate network delay
        return Result.success(
            ScanResult(
                category = "Electronics",
                recyclable = true,
                confidence = 0.86f,
                instructions = listOf("Remove batteries if removable", "Dispose in e-waste bin"),
                instruction = "1. Remove batteries if removable.\n2. Dispose in e-waste bin."
            )
        )
    }

    override suspend fun sendFeedback(feedback: FeedbackRequest): Result<Boolean> {
        delay(500)
        return Result.success(true)
    }
}

class RealScanRepository : ScanRepository {
    override suspend fun scanImage(imageFile: File): Result<ScanResult> {
        return try {
            val requestFile = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("image", imageFile.name, requestFile)
            val response = RetrofitClient.instance.scanImage(body)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Network error: ${response.code()}"))
            }
        } catch (e: Exception) {
            // Fallback to fake for prototype robustness if real fails (optional logic, but per user request "fallback to FakeScanRepository")
            // "if it fails, fallback to FakeScanRepository and mark in logs."
            Log.e("RealScanRepository", "Failed to scan, falling back to mock", e)
            FakeScanRepository().scanImage(imageFile)
        }
    }

    override suspend fun sendFeedback(feedback: FeedbackRequest): Result<Boolean> {
         return try {
            val response = RetrofitClient.instance.sendFeedback(feedback)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                 Result.failure(Exception("Feedback error: ${response.code()}"))
            }
        } catch (e: Exception) {
             Log.e("RealScanRepository", "Failed to feedback", e)
             Result.success(true) // Mock success on failure
        }
    }
}

class LocalModelScanRepository(private val context: android.content.Context) : ScanRepository {
    private val classifier = ImageClassifier(context)

    override suspend fun scanImage(imageFile: File): Result<ScanResult> {
        return try {
            // Convert file to bitmap
            val bitmap = android.graphics.BitmapFactory.decodeFile(imageFile.absolutePath)
            
            // Run inference
            val label = classifier.classify(bitmap)
            
            // Map label to Recyclable status (Mock logic based on label)
            val isRecyclable = when(label) {
                "plastic", "metal", "glass", "paper" -> true
                else -> false // e-waste, textile, other, etc.
            }
            
            // Simulate processing time for UX (scanner animation)
            delay(2000) 

            Result.success(
                ScanResult(
                    category = label.replaceFirstChar { it.uppercase() },
                    recyclable = isRecyclable,
                    confidence = 0.95f, // Mock confidence
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

