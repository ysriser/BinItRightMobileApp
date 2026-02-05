package iss.nus.edu.sg.webviews.binitrightmobileapp

import iss.nus.edu.sg.webviews.binitrightmobileapp.model.LoginResponse
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.LoginRequest
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.User
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.Achievement
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
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Placeholder for future integration of backend
 */
interface ApiService {

    @POST("api/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @GET("api/auth/profile")
    suspend fun getUserProfile(
        @Header("Authorization") token: String
    ): Response<User>

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
    suspend fun getRecycleHistory(): List<RecycleHistoryModel>
    @GET("api/news")
    suspend fun getAllNews(): Response<List<NewsItem>>

    @GET("api/news/{id}")
    suspend fun getNewsById(@Path("id") id: Long): Response<NewsItem>

    @GET("api/achievements/user/{userId}")
    suspend fun getAchievementsWithStatus(
        @Path("userId") userId: Long
    ): Response<List<Achievement>>

    @POST("api/achievements/unlock")
    suspend fun unlockAchievement(
        @Query("userId") userId: Long,
        @Query("achievementId") achievementId: Long
    ): Response<Unit>
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
