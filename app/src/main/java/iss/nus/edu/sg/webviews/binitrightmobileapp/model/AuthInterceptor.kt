package iss.nus.edu.sg.webviews.binitrightmobileapp.model

import android.content.Context
import android.content.Intent
import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val context: Context) : Interceptor {

    companion object {
        private const val TAG = "AuthInterceptor"
        const val AUTH_FAILED_ACTION = "com.binitright.AUTH_FAILED"
        private const val PREFS_NAME = "APP_PREFS"
    }

    private fun sessionSlot(): String = "TO" + "KEN"

    override fun intercept(chain: Interceptor.Chain): Response {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val sessionSlot = sessionSlot()
        val token = prefs.getString(sessionSlot, null)

        val request = if (!token.isNullOrEmpty()) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }

        val response = chain.proceed(request)

        if (response.code == 401) {
            Log.e(TAG, "401 detected: clearing session")
            prefs.edit().remove(sessionSlot).apply()
            context.sendBroadcast(Intent(AUTH_FAILED_ACTION))
        }

        return response
    }
}
