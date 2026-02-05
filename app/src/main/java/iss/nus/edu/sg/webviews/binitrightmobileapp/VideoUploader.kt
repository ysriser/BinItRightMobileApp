package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

object VideoUploader {

    private const val TAG = "VideoUploader"

    suspend fun uploadVideoToSpaces(
        file: File,
        presignedUrl: String,
        onProgress: (Int) -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting upload...")
            Log.d(TAG, "File: ${file.name}, Size: ${file.length()} bytes")
            Log.d(TAG, "URL: ${presignedUrl.substring(0, 100)}...")

            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            val requestBody = file.asRequestBody("video/mp4".toMediaType())

            val request = Request.Builder()
                .url(presignedUrl)
                .put(requestBody)
                .addHeader("Content-Type", "video/mp4")
                .build()

            Log.d(TAG, "##Executing upload request...")

            client.newCall(request).execute().use { response ->
                Log.d(TAG, "##Response code: ${response.code}")
                Log.d(TAG, "##Response message: ${response.message}")

                if (!response.isSuccessful) {
                    val errorBody = response.body?.string()
                    Log.e(TAG, "##Error body: $errorBody")
                }

                if (response.isSuccessful) {
                    Log.d(TAG, "##Video uploaded successfully")
                    onProgress(100)
                    true
                } else {
                    Log.e(TAG, "##Upload failed: ${response.code} - ${response.message}")
                    false
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "##Upload IOException", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "##Upload unexpected error", e)
            false
        }
    }
}