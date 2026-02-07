package iss.nus.edu.sg.webviews.binitrightmobileapp.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Achievement(
    @SerializedName(value = "id", alternate = ["achievementId", "achievement_id"])
    val id: Long,

    val name: String,
    val description: String,
    val criteria: String,

    @SerializedName(value = "badgeIconUrl", alternate = ["badge_icon", "badge_icon_url"])
    val badgeIconUrl: String,

    @SerializedName(value = "unlocked", alternate = ["isUnlocked", "status"])
    var isUnlocked: Boolean = false,

    var dateAchieved: String? = null
) : Serializable