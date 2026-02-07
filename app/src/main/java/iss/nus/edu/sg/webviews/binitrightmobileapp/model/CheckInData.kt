package iss.nus.edu.sg.webviews.binitrightmobileapp.model

data class CheckInData(
    val duration: Long,
    val binId: String,
    val wasteCategory: String,
    val quantity: Int,
    val videoKey: String?
)