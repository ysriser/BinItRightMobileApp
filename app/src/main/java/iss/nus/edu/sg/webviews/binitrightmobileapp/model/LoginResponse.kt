package iss.nus.edu.sg.webviews.binitrightmobileapp.model

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    val success: Boolean,
    val message: String,
    val token: String?,
    @SerializedName("userId")
    val userId: Long?,
    @SerializedName("username")
    val username: String?
)