package iss.nus.edu.sg.webviews.binitrightmobileapp.model
data class EventItem(
    val eventId: Long,
    val title: String,
    val description: String,
    val imageUrl: String,
    val locationName: String,
    val postalCode: String,
    val startTime: String,
    val endTime: String
)