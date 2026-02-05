package iss.nus.edu.sg.webviews.binitrightmobileapp

import iss.nus.edu.sg.webviews.binitrightmobileapp.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("api/auth/profile")
    suspend fun getUserProfile(@Header("Authorization") token: String): Response<User>

    @Multipart
    @POST("api/v1/scan")
    suspend fun scanImage(@Part image: MultipartBody.Part): Response<ScanResult>

    @POST("api/v1/scan/feedback")
    suspend fun sendFeedback(@Body feedback: FeedbackRequest): Response<Boolean>

    @Multipart
    @POST("api/checkin/submit")
    suspend fun submitRecycleCheckIn(
        @Part video: MultipartBody.Part,
        @Part("metadata") metadata: RequestBody
    ): Response<CheckInDataResponse>

    @GET("api/news")
    suspend fun getAllNews(): Response<List<NewsItem>>

    @GET("api/news/{id}")
    suspend fun getNewsById(@Path("id") id: Long): Response<NewsItem>

    // --- Achievements ---
    @GET("api/achievements/user/{userId}")
    suspend fun getAchievementsWithStatus(@Path("userId") userId: Long): Response<List<Achievement>>

    @POST("api/achievements/unlock")
    suspend fun unlockAchievement(@Query("userId") userId: Long, @Query("achievementId") achievementId: Long): Response<Unit>

    // --- Master Branch Additions (Aligned with current model package) ---
    @GET("api/avatar/accessories")
    suspend fun getMyAccessories(): Response<List<UserAccessory>>

    @POST("api/avatar/equip")
    suspend fun equipAccessory(@Query("id") id: Long): Response<Unit>

    @POST("api/avatar/unequip")
    suspend fun unequipAccessory(@Query("id") id: Long): Response<Unit>

    @GET("api/events/upcoming")
    suspend fun getUpcomingEvents(): Response<List<EventItem>>

    @GET("api/profile/summary")
    suspend fun getProfileSummary(): Response<UserProfile>

    @GET("api/recycle/history")
    suspend fun getRecycleHistory(): Response<List<RecycleHistoryModel>>

    @POST("api/issue/report")
    suspend fun createIssue(@Body issue: IssueCreateRequest): Response<IssueResponse>
}