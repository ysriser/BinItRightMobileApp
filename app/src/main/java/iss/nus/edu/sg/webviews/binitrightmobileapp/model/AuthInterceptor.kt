package iss.nus.edu.sg.webviews.binitrightmobileapp.model

import android.content.Context
import android.content.Intent
import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor (private val context: Context) : Interceptor {

    companion object {
        private const val TAG = "AuthInterceptor"
        const val AUTH_FAILED_ACTION = "com.binitright.AUTH_FAILED"
        private const val PREFS_NAME = "APP_PREFS"
        private const val TOKEN = "AUTH_TOKEN"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val prefs = context.getSharedPreferences("APP_PREFS", Context.MODE_PRIVATE)
        val token = prefs.getString("TOKEN", null)

        Log.d("AuthInterceptor", "Sending Token: $token")
        val request = if (!token.isNullOrEmpty()) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else chain.request()

        val response = chain.proceed(request)

        // ✅ Check for 401 Unauthorized globally
        if (response.code == 401) {
            Log.e("AuthInterceptor", "401 Detected - Clearing Session")

            // 1. Wipe the invalid token from storage
            prefs.edit().remove("TOKEN").apply()

            // 2. Broadcast the failure so the UI can navigate
            val intent = Intent("com.binitright.AUTH_FAILED")
            context.sendBroadcast(intent)
        }

        return response
    }
}