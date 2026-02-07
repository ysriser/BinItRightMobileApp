package iss.nus.edu.sg.webviews.binitrightmobileapp.model

import com.google.gson.annotations.SerializedName

data class CheckInData(
    val userId: Long,
    val duration: Long,
    val binId: String,
    val wasteCategory: String,
    val quantity: Int,
    val videoKey: String?,
    @SerializedName("checkInTime")
    val checkInTime: String
)