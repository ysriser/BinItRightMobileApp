package iss.nus.edu.sg.webviews.binitrightmobileapp.model

import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName("userId")
    val id: Long? = 0,

    @SerializedName("username")
    val username: String? = ""
)