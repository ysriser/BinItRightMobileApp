package iss.nus.edu.sg.webviews.binitrightmobileapp

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

// Data classes matching Server Spec v0.1

data class ScanResponse(
    val status: String,
    val request_id: String? = null,
    val data: ScanResponseData? = null,
    val code: String? = null,
    val message: String? = null
)

data class ScanResponseData(
    val tier1: Tier1Result? = null,
    val decision: Decision? = null,
    val final: FinalResult,
    val followup: FollowUp? = null,
    val meta: Meta? = null
)

data class Decision(
    val used_tier2: Boolean,
    val reason_codes: List<String> = emptyList()
)

data class FinalResult(
    val category: String, // Critical
    val category_id: String? = null,
    val recyclable: Boolean, // Critical
    val confidence: Float, // Critical
    val instruction: String, // Critical
    val instructions: List<String> = emptyList(), // Recommended
    val disposal_method: String? = null,
    val bin_type: String? = null,
    val rationale_tags: List<String> = emptyList()
)

data class FollowUp(
    val needs_confirmation: Boolean,
    val questions: List<Any> = emptyList() // Flexible for now
)

data class Meta(
    val schema_version: String? = null
)

interface ApiService {
    @Multipart
    @POST("/api/v1/scan")
    suspend fun scanImage(
        @Part image: MultipartBody.Part,
        @Part tier1: MultipartBody.Part? = null // Send as JSON string if needed, or simple string part
    ): Response<ScanResponse>

    @POST("/api/v1/feedback")
    suspend fun sendFeedback(@retrofit2.http.Body feedback: FeedbackRequest): Response<Boolean>
}
