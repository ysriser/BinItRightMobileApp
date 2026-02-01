package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
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
import iss.nus.edu.sg.todo.samplebin.FindBinsAdapter
import iss.nus.edu.sg.webviews.binitrightmobileapp.databinding.FragmentFindRecyclingBinBinding
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.DropOffLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL

class FindRecyclingBinFragment : Fragment(R.layout.fragment_find_recycling_bin), OnMapReadyCallback {

    private var _binding: FragmentFindRecyclingBinBinding? = null
    private val binding get() = _binding!!

    private lateinit var googleMap: GoogleMap
    private var pendingBins: List<DropOffLocation>? = null
    private var isMapReady = false
    private var lastCameraMoveToken = 0
    private val allBins = mutableListOf<DropOffLocation>()
    private val filteredBins = mutableListOf<DropOffLocation>()
    private lateinit var adapter: FindBinsAdapter
    private lateinit var locationClient: FusedLocationProviderClient
    private var hasFetchedInitialBins = false

    companion object {
        private const val TAG = "FindRecyclingBinFrag"
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val DEFAULT_LAT = 1.28797431732068
        private const val DEFAULT_LNG = 103.805808773107998
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentFindRecyclingBinBinding.bind(view)

        locationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        binding.btnBackToHome.setOnClickListener {
            findNavController().navigateUp()
        }

        setupMap()
        setupRecyclerView()
        setupChipFiltering()
    }

    // -----------------------------
    // Map
    // -----------------------------
    private fun setupMap() {
        val mapFragment = childFragmentManager.findFragmentById(R.id.mapContainer)
                as? SupportMapFragment ?: SupportMapFragment.newInstance().also {
            childFragmentManager.beginTransaction()
                .replace(R.id.mapContainer, it)
                .commit()
        }
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        isMapReady = true

        googleMap.uiSettings.isZoomControlsEnabled = true

        pendingBins?.let {
            updateMapMarkers(it, lastCameraMoveToken)
            pendingBins = null
        }

        // Fetch initial bins only once
        if (!hasFetchedInitialBins) {
            hasFetchedInitialBins = true
            fetchBinsWithLocation("")
        }
    }

    // -----------------------------
    // RecyclerView
    // -----------------------------
    private fun setupRecyclerView() {
        binding.binsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = FindBinsAdapter(filteredBins)
        binding.binsRecyclerView.adapter = adapter
    }

    // -----------------------------
    // Fetch All Bins From Backend
    // -----------------------------
    private fun fetchBinsWithLocation(binType: String) {
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
                val userLat = location?.latitude ?: DEFAULT_LAT
                val userLng = location?.longitude ?: DEFAULT_LNG

                Log.d(TAG, "Fetching bins with lat=$userLat, lng=$userLng, binType=$binType")
                performBinsFetch(userLat, userLng, binType)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to get location", e)
                // Use default location on failure
                performBinsFetch(DEFAULT_LAT, DEFAULT_LNG, binType)
            }
    }

    private fun performBinsFetch(lat: Double, lng: Double, binType: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val json = withContext(Dispatchers.IO) {
                    // FIX 1: Build URL properly with proper empty string handling
                    val urlString = if (binType.isEmpty()) {
                        "http://10.0.2.2:8082/api/bins/all?lat=$lat&lng=$lng"
                    } else {
                        "http://10.0.2.2:8082/api/bins/all?lat=$lat&lng=$lng&binType=$binType"
                    }

                    Log.d(TAG, "Fetching from URL: $urlString")

                    val url = URL(urlString)
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

                        // FIX 2: Add detailed logging
                        Log.d(TAG, "Response length: ${responseText.length}")
                        Log.d(TAG, "Response preview: ${responseText.take(300)}")

                        responseText
                    } finally {
                        connection.disconnect()
                    }
                }

                // FIX 3: Switch back to main thread before parsing/updating UI
                withContext(Dispatchers.Main) {
                    parseAllBinsJson(json)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error fetching bins: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Failed to load bins: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // -----------------------------
    // Parse Backend Response
    // -----------------------------
    private fun parseAllBinsJson(json: String) {
        try {
            Log.d(TAG, "Starting to parse JSON...")

            val jsonArray = org.json.JSONArray(json)
            Log.d(TAG, "JSON array length: ${jsonArray.length()}")

            val tempList = mutableListOf<DropOffLocation>()

            for (i in 0 until jsonArray.length()) {
                try {
                    val obj = jsonArray.getJSONObject(i)

                    val bin = DropOffLocation(
                        id = obj.optLong("id", -1),
                        name = obj.optString("name", "Unknown Bin"),
                        address = obj.optString("address", ""),
                        description = obj.optString("description", ""),
                        postalCode = obj.optString("postalCode", ""),
                        binType = obj.optString("binType", "Unknown"),
                        status = obj.optString("status", "ACTIVE") == "ACTIVE",
                        latitude = obj.optDouble("latitude", 0.0),
                        longitude = obj.optDouble("longitude", 0.0),
                        distanceMeters = obj.optDouble("distanceMeters", 0.0)
                    )

                    tempList.add(bin)
                    Log.d(TAG, "Parsed bin ${i + 1}: ${bin.name} at (${bin.latitude}, ${bin.longitude})")

                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing bin at index $i: ${e.message}", e)
                }
            }

            if (!isAdded) {
                Log.w(TAG, "Fragment not added, skipping UI update")
                return
            }

            Log.d(TAG, "Successfully parsed ${tempList.size} bins")

            allBins.clear()
            allBins.addAll(tempList)

            updateUI()

        } catch (e: Exception) {
            Log.e(TAG, "Error parsing JSON: ${e.message}", e)
            Toast.makeText(requireContext(), "Error parsing bins data", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUI() {
        Log.d(TAG, "Updating UI with ${allBins.size} bins")

        filteredBins.clear()
        filteredBins.addAll(allBins)

        adapter.notifyDataSetChanged()
        Log.d(TAG, "RecyclerView updated")

        lastCameraMoveToken++
        updateMapMarkers(filteredBins, lastCameraMoveToken)
    }

    // -----------------------------
    // Apply Filter (kept for potential future use)
    // -----------------------------
    private fun applyFilter(type: String) {
        Log.d(TAG, "Applying filter: $type")

        filteredBins.clear()

        if (type == "ALL") {
            filteredBins.addAll(allBins)
        } else {
            filteredBins.addAll(
                allBins.filter { it.binType.equals(type, true) }
            )
        }

        Log.d(TAG, "Filtered to ${filteredBins.size} bins")
        adapter.notifyDataSetChanged()

        lastCameraMoveToken++
        updateMapMarkers(filteredBins, lastCameraMoveToken)
    }

    // -----------------------------
    // Map Markers
    // -----------------------------
    private fun updateMapMarkers(list: List<DropOffLocation>, cameraToken: Int) {
        if (!isMapReady) {
            Log.w(TAG, "Map not ready, storing bins for later")
            pendingBins = list
            return
        }

        Log.d(TAG, "Updating map with ${list.size} markers")
        googleMap.clear()

        val limited = list.take(40)  // show only 40 nearest bins
        Log.d(TAG, "Adding ${limited.size} markers to map")

        limited.forEachIndexed { index, bin ->
            try {
                googleMap.addMarker(
                    MarkerOptions()
                        .position(LatLng(bin.latitude, bin.longitude))
                        .title(bin.name)
                        .snippet("${bin.binType} - ${bin.address}")
                )
                Log.d(TAG, "Added marker $index: ${bin.name}")
            } catch (e: Exception) {
                Log.e(TAG, "Error adding marker for ${bin.name}: ${e.message}", e)
            }
        }

        if (limited.isNotEmpty()) {
            val nearest = limited.first()
            Log.d(TAG, "Moving camera to nearest bin: ${nearest.name}")
            googleMap.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(nearest.latitude, nearest.longitude),
                    14f
                )
            )
        } else {
            Log.w(TAG, "No bins to display on map")
        }
    }

    private fun moveCamera(
        bins: List<DropOffLocation>,
        tokenAtRequest: Int
    ) {
        if (bins.isEmpty()) return

        // Wait until map finishes rendering
        googleMap.setOnMapLoadedCallback {
            if (tokenAtRequest != lastCameraMoveToken) return@setOnMapLoadedCallback

            val boundsBuilder = com.google.android.gms.maps.model.LatLngBounds.Builder()

            bins.forEach {
                boundsBuilder.include(
                    LatLng(it.latitude, it.longitude)
                )
            }

            val bounds = boundsBuilder.build()

            googleMap.moveCamera(
                CameraUpdateFactory.newLatLngBounds(bounds, 120)
            )
        }
    }

    // -----------------------------
    // Chip Filtering
    // -----------------------------
    private fun setupChipFiltering() {
        binding.apply {
            chipAll.setOnClickListener {
                chipAll.isChecked = true
                Log.d(TAG, "All chip selected")
                fetchBinsWithLocation("")
            }

            chipBlueBin.setOnClickListener {
                chipBlueBin.isChecked = true
                Log.d(TAG, "BlueBin chip selected")
                fetchBinsWithLocation("BlueBin")  // FIX 6: Match exact backend binType
            }

            chipEwaste.setOnClickListener {
                chipEwaste.isChecked = true
                Log.d(TAG, "EWaste chip selected")
                fetchBinsWithLocation("EWaste")  // FIX 7: Match exact backend binType
            }

            chipLighting.setOnClickListener {
                chipLighting.isChecked = true
                Log.d(TAG, "Lamp chip selected")
                fetchBinsWithLocation("Lamp")  // FIX 8: Match exact backend binType
            }
        }
    }

    // -----------------------------
    // Permission Result
    // -----------------------------
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
            fetchBinsWithLocation("")
        } else {
            Log.w(TAG, "Location permission denied")
            // Fetch with default location
            performBinsFetch(DEFAULT_LAT, DEFAULT_LNG, "")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}