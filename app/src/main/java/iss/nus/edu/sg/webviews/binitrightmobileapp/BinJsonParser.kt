package iss.nus.edu.sg.webviews.binitrightmobileapp

import com.google.gson.Gson
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.DropOffLocation

/**
 * Simple JSON parser for recycling bin lists.
 * Used by "Find Nearby Bins" feature so parsing can be unit tested.
 */
object BinJsonParser {

    fun parse(json: String): List<DropOffLocation> {
        val gson = Gson()
        val dtoArray = gson.fromJson(json, Array<DropOffLocationDto>::class.java).toList()

        return dtoArray.map { dto ->
            DropOffLocation(
                id = dto.id,
                name = dto.name,
                address = dto.address,
                description = dto.description,
                postalCode = dto.postalCode,
                binType = dto.binType,
                status = dto.status,
                latitude = dto.latitude,
                longitude = dto.longitude,
                distanceMeters = dto.distanceMeters ?: 0.0,
            )
        }
    }

    private data class DropOffLocationDto(
        val id: String,
        val name: String,
        val address: String,
        val description: String,
        val postalCode: String,
        val binType: String,
        val latitude: Double,
        val longitude: Double,
        val status: Boolean,
        val distanceMeters: Double?,
    )
}
