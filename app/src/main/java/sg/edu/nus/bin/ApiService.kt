package sg.edu.nus.bin

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

// This retrofit interface defines multipart/form-data endpoint
// It accepts both videofile and JSON metadata in seperate parts
interface ApiService {

    @Multipart
    @POST("/api/checkin/submit")
    suspend fun submitRecycleCheckIn(
        @Part video: MultipartBody.Part,
        @Part("metadata") metadata: RequestBody
        ): Response<CheckInDataResponse>
}