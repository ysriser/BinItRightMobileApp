package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import iss.nus.edu.sg.webviews.binitrightmobileapp.databinding.FragmentNearByBinBinding
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.DropOffLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

class NearByBinFragment : Fragment(R.layout.fragment_near_by_bin), OnMapReadyCallback {

    private var _binding: FragmentNearByBinBinding? = null
    private val binding get() = _binding!!

    private lateinit var googleMap: GoogleMap
    private lateinit var locationClient: FusedLocationProviderClient

    private val nearbyBins = mutableListOf<DropOffLocation>()
    private var adapter: NearByBinsAdapter? = null

    private var selectedBinType: String? = null
    private var wasteCategory: String? = null

    private var pendingBins: List<DropOffLocation>? = null
    private var isMapReady = false
    private var hasZoomedToBins = false
    private var hasFetchedBins = false

    companion object {
        private const val TAG = "NearByBinFragment"
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val FALLBACK_LAT = 1.3521
        private const val FALLBACK_LNG = 103.8198
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentNearByBinBinding.bind(view)

        locationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        retrieveScanArguments()
        setupMap()
        setupRecyclerView()
    }

    private fun retrieveScanArguments() {
        arguments?.let { bundle ->
            // Keep both keys for backward compatibility.
            selectedBinType = bundle.getString("selectedBinType") ?: bundle.getString("binType")
            wasteCategory = bundle.getString("wasteCategory") ?: bundle.getString("scannedCategory")
        }

        Log.d(TAG, "selectedBinType=$selectedBinType, wasteCategory=$wasteCategory")
    }

    private fun setupMap() {
        val mapFragment = childFragmentManager.findFragmentById(R.id.mapContainer)
            as? SupportMapFragment ?: SupportMapFragment.newInstance().also { fragment ->
            childFragmentManager.beginTransaction()
                .replace(R.id.mapContainer, fragment)
                .commit()
        }

        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        isMapReady = true

        googleMap.uiSettings.isZoomControlsEnabled = true

        pendingBins?.let {
            updateMapMarkers(it)
            pendingBins = null
        }

        fetchUserLocation()
    }

    private fun setupRecyclerView() {
        binding.binsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = NearByBinsAdapter(nearbyBins) { selectedBin ->
            navigateToCheckIn(selectedBin)
        }
        binding.binsRecyclerView.adapter = adapter
    }

    private fun navigateToCheckIn(bin: DropOffLocation) {
        val bundle = Bundle().apply {
            putString("binId", bin.id)
            putString("binName", bin.name)
            putString("binAddress", bin.address)
            putString("binType", bin.binType)
            putDouble("binLatitude", bin.latitude)
            putDouble("binLongitude", bin.longitude)
        }

        selectedBinType?.let {
            bundle.putString("selectedBinType", it)
        }
        wasteCategory?.let {
            bundle.putString("wasteCategory", it)
            bundle.putString("scannedCategory", it)
        }

        try {
            findNavController().navigate(
                R.id.action_nearByBinFragment_to_checkInFragment,
                bundle
            )
        } catch (e: Exception) {
            Log.e(TAG, "Navigation to check-in failed: ${e.message}", e)
        }
    }

    private fun fetchUserLocation() {
        if (hasFetchedBins) {
            return
        }

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        locationClient.lastLocation
            .addOnSuccessListener { location ->
                if (hasFetchedBins) return@addOnSuccessListener

                val lat = location?.latitude ?: FALLBACK_LAT
                val lng = location?.longitude ?: FALLBACK_LNG
                hasFetchedBins = true
                fetchNearbyBins(lat, lng)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to get location: ${e.message}", e)
                hasFetchedBins = true
                fetchNearbyBins(FALLBACK_LAT, FALLBACK_LNG)
            }
    }

    private fun fetchNearbyBins(lat: Double, lng: Double) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val json = withContext(Dispatchers.IO) {
                    val normalizedBinType = normalizeBinType(selectedBinType)
                    val urlString = buildBinsSearchUrl(lat, lng, normalizedBinType)

                    Log.d(TAG, "Fetching bins URL: $urlString")

                    val connection = java.net.URL(urlString).openConnection() as java.net.HttpURLConnection
                    try {
                        connection.requestMethod = "GET"
                        connection.connectTimeout = 10000
                        connection.readTimeout = 10000

                        val responseCode = connection.responseCode
                        val stream = if (responseCode in 200..299) {
                            connection.inputStream
                        } else {
                            Log.e(TAG, "Bins API error: $responseCode")
                            connection.errorStream
                        }

                        BufferedReader(InputStreamReader(stream)).use { it.readText() }
                    } finally {
                        connection.disconnect()
                    }
                }

                parseBinsJson(json)
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching nearby bins: ${e.message}", e)
            }
        }
    }

    private fun buildBinsSearchUrl(lat: Double, lng: Double, normalizedBinType: String?): String {
        val base = BuildConfig.BASE_URL.trimEnd('/')
        val query = if (normalizedBinType.isNullOrBlank()) {
            "lat=$lat&lng=$lng&radius=3000"
        } else {
            "lat=$lat&lng=$lng&radius=3000&binType=$normalizedBinType"
        }
        return "$base/api/bins/search?$query"
    }

    private fun normalizeBinType(raw: String?): String? {
        if (raw.isNullOrBlank()) {
            return null
        }

        val value = raw.trim().uppercase()
        return when {
            value.contains("BLUE") -> "BLUEBIN"
            value.contains("LIGHT") || value.contains("LAMP") -> "Lighting"
            value.contains("EWASTE") || value.contains("E-WASTE") -> "EWaste"
            else -> null
        }
    }

    private fun parseBinsJson(json: String) {
        try {
            val trimmed = json.trim()
            if (!trimmed.startsWith("[") && !trimmed.startsWith("{")) {
                Log.e(TAG, "Bins response is not valid JSON")
                return
            }

            val jsonArray = org.json.JSONArray(json)
            val parsedBins = mutableListOf<DropOffLocation>()

            for (i in 0 until jsonArray.length()) {
                try {
                    val obj = jsonArray.getJSONObject(i)

                    val statusString = obj.optString("status", "ACTIVE")
                    val statusBoolean = statusString.equals("ACTIVE", ignoreCase = true)
                    val distance = if (obj.isNull("distanceMeters")) 0.0 else obj.optDouble("distanceMeters", 0.0)

                    val bin = DropOffLocation(
                        id = obj.optString("id", ""),
                        name = obj.optString("name", "Unknown Bin"),
                        address = obj.optString("address", ""),
                        description = obj.optString("description", ""),
                        postalCode = obj.optString("postalCode", ""),
                        binType = obj.optString("binType", "Unknown"),
                        status = statusBoolean,
                        latitude = obj.optDouble("latitude", 0.0),
                        longitude = obj.optDouble("longitude", 0.0),
                        distanceMeters = distance
                    )

                    parsedBins.add(bin)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse bin at index $i: ${e.message}", e)
                }
            }

            if (!isAdded) {
                return
            }

            nearbyBins.clear()
            nearbyBins.addAll(parsedBins)
            adapter?.notifyDataSetChanged()
            updateMapMarkers(parsedBins)

            Log.d(TAG, "Parsed bins: ${parsedBins.size}")
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing bins JSON: ${e.message}", e)
        }
    }

    private fun updateMapMarkers(bins: List<DropOffLocation>) {
        if (!isMapReady) {
            pendingBins = bins
            return
        }

        if (bins.isEmpty()) {
            Log.w(TAG, "No bins to display on map")
            return
        }

        googleMap.clear()

        bins.forEach { bin ->
            try {
                val position = LatLng(bin.latitude, bin.longitude)
                val distanceText = if (bin.distanceMeters > 0) {
                    "${bin.distanceMeters.toInt()} m"
                } else {
                    "Distance unknown"
                }

                googleMap.addMarker(
                    MarkerOptions()
                        .position(position)
                        .title(bin.name)
                        .snippet("${bin.binType} - $distanceText")
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error adding map marker: ${e.message}", e)
            }
        }

        if (!hasZoomedToBins) {
            val first = bins.first()
            googleMap.moveCamera(
                CameraUpdateFactory.newLatLngZoom(LatLng(first.latitude, first.longitude), 16f)
            )
            hasZoomedToBins = true
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (
            requestCode == LOCATION_PERMISSION_REQUEST_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            fetchUserLocation()
        } else {
            // Still show bins using a fallback coordinate so feature does not dead-end.
            hasFetchedBins = true
            fetchNearbyBins(FALLBACK_LAT, FALLBACK_LNG)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

