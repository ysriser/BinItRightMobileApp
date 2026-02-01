package iss.nus.edu.sg.webviews.binitrightmobileapp.model

import com.google.gson.annotations.SerializedName

data class NewsItem(
    @SerializedName("news_Id")
    val id: Long,

    @SerializedName("name")
    val title: String,

    @SerializedName("description")
    val description: String,

    @SerializedName("image_url")
    val imageUrl: String?,

    @SerializedName("status")
    val status: String
)