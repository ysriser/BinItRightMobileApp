package iss.nus.edu.sg.webviews.binitrightmobileapp

import iss.nus.edu.sg.webviews.binitrightmobileapp.model.DropOffLocation
import org.json.JSONArray

/**
 * Simple JSON parser for recycling bin lists.
 * Used by "Find Nearby Bins" feature so parsing can be unit tested.
 */
object BinJsonParser {

    fun parse(json: String): List<DropOffLocation> {
        val list = mutableListOf<DropOffLocation>()
        val jsonArray = JSONArray(json)

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)

            list.add(
                DropOffLocation(
                    id = obj.getLong("id"),
                    name = obj.getString("name"),
                    address = obj.getString("address"),
                    description = obj.getString("description"),
                    postalCode = obj.getString("postalCode"),
                    binType = obj.getString("binType"),
                    status = obj.getBoolean("status"),
                    latitude = obj.getDouble("latitude"),
                    longitude = obj.getDouble("longitude"),
                    distanceMeters = obj.optDouble("distanceMeters", 0.0)
                )
            )
        }

        return list
    }
}
