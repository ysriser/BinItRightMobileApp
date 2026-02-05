package iss.nus.edu.sg.webviews.binitrightmobileapp

data class CheckInData(
    val userId: Long,
    val duration: Long,
    val binId: String,
    val wasteCategory: String,
    val quantity: Int,
    val videoKey: String?
)
