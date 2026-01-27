package sg.edu.nus.bin

object LocationChecker{
    fun isWithinRadius(
        userLat: Double,
        userLng: Double,
        binLat: Double,
        binLng: Double,
        radius: Double
    ): Boolean{
        val result = FloatArray(1)

        // calculates distance between two coordinates on Earth's surface
        //using haversine formula and writes distance to result[0]
        android.location.Location.distanceBetween(
            userLat, userLng,
            binLat, binLng,
            result
        )

        return result[0] <= radius
    }
}