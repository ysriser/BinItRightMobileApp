package iss.nus.edu.sg.webviews.binitrightmobileapp.Model

data class DropOffLocation(
    val id: Long,
    val name: String,
    val address: String,
    val postalCode: String,
    val description: String,
    val binType: String,
    val latitude: Double,
    val longitude: Double,
    val status: Boolean,
    val distanceMeters: Double,

)