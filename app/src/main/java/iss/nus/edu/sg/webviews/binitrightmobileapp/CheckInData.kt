package iss.nus.edu.sg.webviews.binitrightmobileapp

data class CheckInData(
    val userId: Int,
    val recordedAt: Long,
    val duration: Int,
    val binId: Int,
    val wasteCategory: String,
    val quantity: Int
)
