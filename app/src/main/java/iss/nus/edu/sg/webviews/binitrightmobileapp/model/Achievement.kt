package iss.nus.edu.sg.webviews.binitrightmobileapp.model

import com.google.gson.annotations.SerializedName

data class Achievement(
    @SerializedName("achievement_id")
    val id: Long,

    @SerializedName("name")
    val name: String,

    @SerializedName("description")
    val description: String,

    @SerializedName("criteria")
    val criteria: String,

    @SerializedName("badge_icon")
    val badgeIconUrl: String,

    val isUnlocked: Boolean = false,

    val dateAchieved: String? = null
)