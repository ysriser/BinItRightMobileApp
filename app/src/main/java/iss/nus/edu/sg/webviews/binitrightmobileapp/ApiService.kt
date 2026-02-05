package iss.nus.edu.sg.webviews.binitrightmobileapp

import iss.nus.edu.sg.webviews.binitrightmobileapp.model.IssueCreateRequest
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.IssueResponse
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.LoginResponse
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.LoginRequest
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.EventItem
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.NewsItem
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.RecycleHistoryModel
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.UserAccessory
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.UserProfile
import okhttp3.MultipartBody
import okhttp3.RequestBody

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

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

    @POST("api/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>
    @Multipart
    @POST("/api/v1/scan")
    suspend fun scanImage(
        @Part image: MultipartBody.Part,
        @Part tier1: MultipartBody.Part? = null // Send as JSON string if needed, or simple string part
    ): Response<ScanResponse>

    @POST("/api/v1/feedback")
    suspend fun sendFeedback(@retrofit2.http.Body feedback: FeedbackRequest): Response<Boolean>


    @POST("api/checkin")
    suspend fun submitRecycleCheckIn(
        @Body checkInData: CheckInData
    ): Response<CheckInDataResponse>

    // Endpoint for getting pre-signed upload URL
    @POST("api/videos/presign-upload")
    suspend fun getPresignedUpload(
        @Body req: PresignUploadRequest
    ): Response<PresignUploadResponse>

    @GET("api/recycle-history")
    suspend fun getRecycleHistory(): List<RecycleHistoryModel>
    @GET("api/news")
    suspend fun getAllNews(): Response<List<NewsItem>>

    @POST("api/issues")
    suspend fun createIssue(@Body request: IssueCreateRequest): Response<IssueResponse>

    @GET("api/events?filter=upcoming")
    suspend fun getUpcomingEvents(): Response<List<EventItem>>

    @GET("api/user-accessories/my-items")
    suspend fun getMyAccessories(): Response<List<UserAccessory>>

    @POST("api/user-accessories/equip/{id}")
    suspend fun equipAccessory(@Path("id") accessoryId: Long): Response<Void>

    @POST("api/user-accessories/unequip/{id}")
    suspend fun unequipAccessory(@Path("id") id: Long): Response<Void>

    @GET("api/summary/profile")
    suspend fun getProfileSummary(): Response<UserProfile>
}
