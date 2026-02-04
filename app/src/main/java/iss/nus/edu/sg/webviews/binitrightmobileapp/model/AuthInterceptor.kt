package iss.nus.edu.sg.webviews.binitrightmobileapp.model

import android.content.Context
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor (private val context: Context) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val prefs = context.getSharedPreferences("APP_PREFS", Context.MODE_PRIVATE)
        val token = prefs.getString("TOKEN", null)

        val request = if (!token.isNullOrEmpty()) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else chain.request()

        return chain.proceed(request)
    }
}