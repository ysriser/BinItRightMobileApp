package iss.nus.edu.sg.webviews.binitrightmobileapp.model

data class UserProfile(
    val name: String,
    val pointBalance: Int,
    val equippedAvatarName: String,
    val totalRecycled: Int,
    val aiSummary: String,
    val totalAchievement:Int,
    val carbonEmissionSaved: Double
)