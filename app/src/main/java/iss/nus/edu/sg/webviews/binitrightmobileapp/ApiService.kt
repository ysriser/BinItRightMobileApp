package iss.nus.edu.sg.webviews.binitrightmobileapp

import iss.nus.edu.sg.webviews.binitrightmobileapp.model.Achievement
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.Accessory
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.ChatRequest
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.ChatResponse
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.CheckInData
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.DropOffLocation
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.EventItem
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.IssueCreateRequest
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.IssueResponse
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.LoginRequest
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.LoginResponse
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.NewsItem
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.RecycleHistoryModel
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.RedeemResponse
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.RegisterRequest
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.RegisterResponse
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.UserAccessory
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.UserProfile
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.UserResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

// Data classes matching scan server contract v0.1

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
    val final: FinalResult? = null,
    val meta: Meta? = null
)

data class Decision(
    val used_tier2: Boolean,
    val reason_codes: List<String> = emptyList()
)

data class FinalResult(
    val category: String,
    val recyclable: Boolean,
    val confidence: Float,
    val instruction: String,
    val instructions: List<String> = emptyList()
)

data class Tier2Error(
    val http_status: String? = null,
    val code: String? = null,
    val message: String? = null
)

data class Meta(
    val schema_version: String? = null,
    val force_cloud: Boolean? = null,
    val tier2_provider_attempted: String? = null,
    val tier2_provider_used: String? = null,
    val tier2_provider: String? = null,
    val tier2_error: Tier2Error? = null
)

interface ApiService {

    @POST("api/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @POST("api/auth/register")
    suspend fun register(@Body req: RegisterRequest): Response<RegisterResponse>


    @Multipart
    @POST("/api/v1/scan")
    suspend fun scanImage(
        @Part image: MultipartBody.Part,
        @Part("tier1") tier1: RequestBody? = null,
        @Part("force_cloud") forceCloud: RequestBody? = null,
        @Part("timestamp") timestamp: RequestBody? = null
    ): Response<ScanResponse>

    @POST("/api/v1/feedback")
    suspend fun sendFeedback(@Body feedback: FeedbackRequest): Response<Boolean>

    @POST("api/checkin")
    suspend fun submitRecycleCheckIn(
        @Body checkInData: CheckInData
    ): Response<CheckInDataResponse>

    @POST("api/videos/presign-upload")
    suspend fun getPresignedUpload(
        @Body req: PresignUploadRequest
    ): Response<PresignUploadResponse>

    @GET("api/recycle-history")
    suspend fun getRecycleHistory(): List<RecycleHistoryModel>

    @GET("api/news")
    suspend fun getAllNews(): Response<List<NewsItem>>

    @GET("api/news/{id}")
    suspend fun getNewsById(@Path("id") id: Long): Response<NewsItem>

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

    @GET("/api/reward-shop/items")
    suspend fun getRewardShopItems(): Response<List<Accessory>>

    @POST("/api/reward-shop/redeem/{accessoriesId}")
    suspend fun redeemRewardShopItem(
        @Path("accessoriesId") accessoriesId: Long
    ): Response<RedeemResponse>

    @GET("api/summary/profile")
    suspend fun getUserProfile(@Path("id") userId: Long): Response<UserProfile>

    @GET("api/bins/search")
    suspend fun getNearbyBins(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("radius") radius: Int
    ): List<DropOffLocation>

    @POST("api/chat")
    suspend fun chat(@Body req: ChatRequest): ChatResponse

    @GET("api/users/{userId}/total-recycled")
    suspend fun getTotalRecycled(
        @Path("userId") userId: Long
    ): Response<Int>


    @GET("api/achievements/user/{userId}")
    suspend fun getAchievementsWithStatus(@Path("userId") userId: Long): Response<List<Achievement>>

    @POST("api/achievements/unlock/{userId}/{achievementId}")
    suspend fun unlockAchievement(
        @Path("userId") userId: Long,
        @Path("achievementId") achievementId: Long
    ): Response<Unit>
}
