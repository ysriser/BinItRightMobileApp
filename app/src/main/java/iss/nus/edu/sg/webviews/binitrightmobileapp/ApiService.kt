package iss.nus.edu.sg.webviews.binitrightmobileapp

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

/**
 * Placeholder for future integration of backend
 */
interface ApiService {
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
}
