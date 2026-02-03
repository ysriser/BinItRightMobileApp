package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.io.BufferedReader
import java.io.InputStreamReader
import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import iss.nus.edu.sg.webviews.binitrightmobileapp.databinding.FragmentNearByBinBinding
import iss.nus.edu.sg.webviews.binitrightmobileapp.Model.DropOffLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class NearByBinFragment : Fragment(R.layout.fragment_near_by_bin), OnMapReadyCallback{

    private var _binding: FragmentNearByBinBinding? = null
    private val binding get() = _binding!!
    private lateinit var googleMap: GoogleMap
    private var pendingBins: List<DropOffLocation>? = null
    private var isMapReady = false
    private var hasZoomedToBins = false
    private var hasFetchedBins = false
    private val nearbyBins = mutableListOf<DropOffLocation>()
    private var selectedBinType: String? = null
    private var wasteCategory: String? = null
    private var adapter: NearByBinsAdapter?= null
    private lateinit var locationClient: FusedLocationProviderClient

    companion object {
        private const val TAG = "NearByBinFragment"
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentNearByBinBinding.bind(view)
        locationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        retrieveBinType()
        setupMap()
        setupRecyclerView()
    }

    private fun retrieveBinType() {
        arguments?.let { bundle ->
            selectedBinType = bundle.getString("binType")
            if (selectedBinType != null) {
                Log.d(TAG, "Retrieved scanned bin type: $selectedBinType")
            } else {
            }
        } ?: run {
            selectedBinType = null
        }
    }

    // -----------------------------
    // Map
    // -----------------------------
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
        Log.d(TAG, "Map is ready")
        googleMap = map
        isMapReady = true

        googleMap.uiSettings.isZoomControlsEnabled = true

        pendingBins?.let {
            Log.d(TAG, "Displaying ${it.size} pending bins")
            updateMapMarkers(it)
            pendingBins = null
        }

        fetchUserLocation()
    }

    // -----------------------------
    // RecyclerView
    // -----------------------------
    private fun setupRecyclerView() {
        binding.binsRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = NearByBinsAdapter(nearbyBins) { selectedBin ->
            navigateToCheckIn(selectedBin)
        }
        binding.binsRecyclerView.adapter = adapter
        Log.d(TAG, "RecyclerView setup complete")
    }

    private fun navigateToCheckIn(bin: DropOffLocation) {
        Log.d(TAG, "Navigating to check-in for bin: ${bin.name}")

        val bundle = Bundle().apply {
            putLong("binId", bin.id)
            putString("binName", bin.name)
            putString("binAddress", bin.address)
            putString("binType", bin.binType)
            putDouble("binLatitude", bin.latitude)
            putDouble("binLongitude", bin.longitude)
        }

        // Pass the scanned bin type if it exists
        selectedBinType?.let {
            bundle.putString("selectedBinType", it)
        }
        wasteCategory?.let {
            bundle.putString("scannedCategory", it)
        }

        try {
            findNavController().navigate(
                R.id.action_nearByBinFragment_to_checkInFragment,
                bundle
            )
        } catch (e: Exception) {
        }
    }

    private fun fetchUserLocation() {
        if (hasFetchedBins) {
            Log.d(TAG, "Already fetched bins, skipping")
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

                val lat = location?.latitude ?: 1.2879
                val lng = location?.longitude ?: 103.8058

                Log.d(TAG, "Got user location: lat=$lat, lng=$lng")
                hasFetchedBins = true
                fetchNearbyBins(1.29, 103.78)  // Using your default coordinates
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to get location: ${e.message}", e)
                // Use fallback location
                hasFetchedBins = true
                fetchNearbyBins(1.3521, 103.8198)
            }
    }

    // -----------------------------
    // Nearby Bins based on user location
    // -----------------------------
    private fun fetchNearbyBins(lat: Double, lng: Double) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                Log.d(TAG, "Starting to fetch nearby bins for lat=$lat, lng=$lng")

                val json = withContext(Dispatchers.IO) {
                    // Use emulator URL (10.0.2.2) for Android Emulator
                    // Use device URL (192.168.88.4) for physical device
                    val urlString = "http://10.0.2.2:8080/api/bins/nearby?lat=$lat&lng=$lng&radius=3000"

                    Log.d(TAG, "Fetching from URL: $urlString")

                    val url = java.net.URL(urlString)
                    val connection = url.openConnection() as java.net.HttpURLConnection

                    try {
                        connection.requestMethod = "GET"
                        connection.connectTimeout = 10000
                        connection.readTimeout = 10000

                        val responseCode = connection.responseCode
                        Log.d(TAG, "Response code: $responseCode")

                        val stream = if (responseCode in 200..299) {
                            Log.d(TAG, "###Stream incoming")
                            connection.inputStream
                        } else {
                            Log.e(TAG, "HTTP Error: $responseCode")
                            connection.errorStream
                        }

                        val responseText = BufferedReader(InputStreamReader(stream)).use { it.readText() }

                        Log.d(TAG, "Response length: ${responseText.length}")

                        responseText
                    } finally {
                        connection.disconnect()
                    }
                }

                Log.d(TAG, "Response JSON = $json")

                // Parse on Main thread
                withContext(Dispatchers.Main) {
                    parseBinsJson(json)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error fetching nearby bins: ${e.message}", e)
                withContext(Dispatchers.Main) {

                }
            }
        }
    }

    // -----------------------------
    // Parse Response
    // -----------------------------
    private fun parseBinsJson(json: String) {
        try {
            val trimmed = json.trim()
            if (!trimmed.startsWith("[") && !trimmed.startsWith("{")) {
                Log.e(TAG, "Response is not JSON. First 200 chars: ${trimmed.take(200)}")
                throw Exception("Invalid response from server (not JSON)")
            }
            Log.d(TAG, "Starting to parse JSON...")

            val jsonArray = org.json.JSONArray(json)
            Log.d(TAG, "JSON array length: ${jsonArray.length()}")

            val parsedBins = mutableListOf<DropOffLocation>()

            for (i in 0 until jsonArray.length()) {
                try {
                    val obj = jsonArray.getJSONObject(i)

                    // CRITICAL FIX: Parse status as STRING first, then convert to boolean
                    val statusString = obj.optString("status", "ACTIVE")
                    val statusBoolean = statusString.equals("ACTIVE", ignoreCase = true)

                    // CRITICAL FIX: Handle null distanceMeters
                    val distance = if (obj.isNull("distanceMeters")) {
                        0.0
                    } else {
                        obj.optDouble("distanceMeters", 0.0)
                    }

                    val bin = DropOffLocation(
                        id = obj.optLong("id", -1),
                        name = obj.optString("name", "Unknown Bin"),
                        address = obj.optString("address", ""),
                        description = obj.optString("description", ""),
                        postalCode = obj.optString("postalCode", ""),
                        binType = obj.optString("binType", "Unknown"),
                        status = statusBoolean,  // Use converted boolean
                        latitude = obj.optDouble("latitude", 0.0),
                        longitude = obj.optDouble("longitude", 0.0),
                        distanceMeters = distance  // Use null-safe distance
                    )

                    parsedBins.add(bin)
                    Log.d(TAG, "Parsed bin ${i + 1}: ${bin.name} at (${bin.latitude}, ${bin.longitude}), distance=${bin.distanceMeters}m")

                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing bin at index $i: ${e.message}", e)
                }
            }

            if (!isAdded) {
                Log.w(TAG, "Fragment not added, skipping UI update")
                return
            }

            Log.d(TAG, "Successfully parsed ${parsedBins.size} bins")

            nearbyBins.clear()
            nearbyBins.addAll(parsedBins)

            Log.d(TAG, "Notifying adapter of ${nearbyBins.size} bins")
            adapter?.notifyDataSetChanged()

            Log.d(TAG, "Updating map markers")
            updateMapMarkers(parsedBins)

        } catch (e: Exception) {
            Log.e(TAG, "Error parsing JSON: ${e.message}", e)

        }
    }


    // -----------------------------
    // Mark location in map
    // -----------------------------
    private fun updateMapMarkers(bins: List<DropOffLocation>) {
        Log.d(TAG, "updateMapMarkers called with ${bins.size} bins")

        if (!isMapReady) {
            Log.w(TAG, "Map not ready, storing bins for later")
            pendingBins = bins
            return
        }

        if (bins.isEmpty()) {
            Log.w(TAG, "No bins to display on map")
            return
        }

        Log.d(TAG, "Clearing existing markers")
        googleMap.clear()

        Log.d(TAG, "Adding ${bins.size} markers to map")
        bins.forEachIndexed { index, bin ->
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

                Log.d(TAG, "Added marker $index: ${bin.name} at $position")
            } catch (e: Exception) {
                Log.e(TAG, "Error adding marker for ${bin.name}: ${e.message}", e)
            }
        }

        if (!hasZoomedToBins && bins.isNotEmpty()) {
            val first = bins.first()
            val position = LatLng(first.latitude, first.longitude)

            Log.d(TAG, "Moving camera to first bin: ${first.name} at $position")
            googleMap.moveCamera(
                CameraUpdateFactory.newLatLngZoom(position, 16f)
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

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(TAG, "Location permission granted")
            fetchUserLocation()
        } else {
            Log.w(TAG, "Location permission denied")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
