package iss.nus.edu.sg.webviews.binitrightmobileapp

import iss.nus.edu.sg.webviews.binitrightmobileapp.model.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

/**
 * Placeholder for future integration of backend
 */
interface ApiService {

    @POST("api/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @Multipart
    @POST("api/v1/scan")
    suspend fun scanImage(@Part image: MultipartBody.Part): Response<ScanResult>

    @POST("api/v1/scan/feedback")
    suspend fun sendFeedback(@Body feedback: FeedbackRequest): Response<Boolean>

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
    suspend fun getRecycleHistory(): Response<List<RecycleHistoryModel>>

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
    suspend fun equipAccessory(@Path("id") accessoryId: Long): Response<Unit>

    @POST("api/user-accessories/unequip/{id}")
    suspend fun unequipAccessory(@Path("id") id: Long): Response<Unit>

    @GET("api/summary/profile")
    suspend fun getProfileSummary(): Response<UserProfile>

    @GET("/api/reward-shop/items")
    suspend fun getRewardShopItems(): Response<List<Accessory>>

    @POST("/api/reward-shop/redeem/{accessoriesId}")
    suspend fun redeemRewardShopItem(
        @Path("accessoriesId") accessoriesId: Long
    ): Response<RedeemResponse>

    @GET("api/user/profile/{id}")
    suspend fun getUserProfile(@Path("id") userId: Long): Response<UserResponse>

    @GET("api/achievements/user/{userId}")
    suspend fun getAchievementsWithStatus(@Path("userId") userId: Long): Response<List<Achievement>>

    @POST("api/achievements/unlock/{userId}/{achievementId}")
    suspend fun unlockAchievement(
        @Path("userId") userId: Long,
        @Path("achievementId") achievementId: Long
    ): Response<Unit>
}