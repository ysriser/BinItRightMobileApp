package iss.nus.edu.sg.webviews.binitrightmobileapp.model

data class CheckInData(
    val userId: Long,
    val duration: Long,
    val binId: Long,
    val wasteCategory: String,
    val quantity: Int,
    val videoKey: String?,
    val checkInTime: String
)