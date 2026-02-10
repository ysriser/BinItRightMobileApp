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
            safeLogDebug("Starting upload...")
            safeLogDebug("File: ${file.name}, Size: ${file.length()} bytes")
            safeLogDebug("URL: ${presignedUrl.take(100)}...")

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

            safeLogDebug("##Executing upload request...")

            client.newCall(request).execute().use { response ->
                safeLogDebug("##Response code: ${response.code}")
                safeLogDebug("##Response message: ${response.message}")

                if (!response.isSuccessful) {
                    val errorBody = response.body?.string()
                    safeLogError("##Error body: $errorBody")
                }

                if (response.isSuccessful) {
                    safeLogDebug("##Video uploaded successfully")
                    onProgress(100)
                    true
                } else {
                    safeLogError("##Upload failed: ${response.code} - ${response.message}")
                    false
                }
            }
        } catch (e: IOException) {
            safeLogError("##Upload IOException", e)
            false
        } catch (e: Exception) {
            safeLogError("##Upload unexpected error", e)
            false
        }
    }

    private fun safeLogDebug(message: String) {
        runCatching { Log.d(TAG, message) }
    }

    private fun safeLogError(message: String, throwable: Throwable? = null) {
        runCatching {
            if (throwable != null) {
                Log.e(TAG, message, throwable)
            } else {
                Log.e(TAG, message)
            }
        }
    }
}
