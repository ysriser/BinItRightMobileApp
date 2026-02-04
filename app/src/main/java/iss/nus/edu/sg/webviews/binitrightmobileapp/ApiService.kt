package iss.nus.edu.sg.webviews.binitrightmobileapp

import iss.nus.edu.sg.webviews.binitrightmobileapp.Model.LoginResponse
import iss.nus.edu.sg.webviews.binitrightmobileapp.Model.LoginRequest
import okhttp3.MultipartBody
import okhttp3.RequestBody

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

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

    @GET("api/events?filter=upcoming")
    suspend fun getUpcomingEvents(): Response<List<EventItem>>
}
