package iss.nus.edu.sg.webviews.binitrightmobileapp.utils

import android.util.Base64
import org.json.JSONObject

object JwtUtils {

    fun getUserIdFromToken(token: String): Long? {
        return try {
            val parts = token.split(".")
            if (parts.size != 3) return null

            val payload = String(
                Base64.decode(parts[1], Base64.URL_SAFE)
            )

            val json = JSONObject(payload)
            json.getString("sub").toLong()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getRoleFromToken(token: String): String? {
        return try {
            val payload = String(
                Base64.decode(token.split(".")[1], Base64.URL_SAFE)
            )
            JSONObject(payload).getString("role")
        } catch (e: Exception) {
            null
        }
    }
}
