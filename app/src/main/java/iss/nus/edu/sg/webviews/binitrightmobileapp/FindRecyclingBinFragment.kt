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
                    // Build URL from the same environment base URL used by login/API calls
                    val baseUrl = BuildConfig.BASE_URL.trimEnd('/')
                    val urlString = if (binType.isEmpty()) {
                        "$baseUrl/api/bins/search?lat=$lat&lng=$lng"
                    } else {
                        "$baseUrl/api/bins/search?lat=$lat&lng=$lng&binType=$binType"
                    }

                    val url = URL(urlString)
                    val connection = url.openConnection() as java.net.HttpURLConnection


                    try {
                        connection.requestMethod = "GET"
                        connection.connectTimeout = 10000
                        connection.readTimeout = 10000

                        val stream = if (connection.responseCode in 200..299) {
                            connection.inputStream
                        } else {
                            Log.e(TAG, "HTTP Error: ${connection.responseCode}")
                            connection.errorStream
                        }

                        BufferedReader(InputStreamReader(stream)).use { it.readText() }
                    } finally {
                        connection.disconnect()
                    }
                }

                withContext(Dispatchers.Main) {
                    parseAllBinsJson(json)
                }
            } catch (e: Exception) {
                // You might want to show an error message to the user here
                // For example: Toast.makeText(requireContext(), "Failed to load bins", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // -----------------------------
    // Parse Backend Response
    // -----------------------------
    private fun parseAllBinsJson(json: String) {
        try {
            val jsonArray = org.json.JSONArray(json)
            val tempList = mutableListOf<DropOffLocation>()

            for (i in 0 until jsonArray.length()) {
                try {
                    val obj = jsonArray.getJSONObject(i)

                    val bin = DropOffLocation(
                        id = obj.optString("id", ""),
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
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing bin at index $i: ${e.message}", e)
                }
            }

            if (!isAdded) {
                return
            }

            allBins.clear()
            allBins.addAll(tempList)

            updateUI()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateUI() {

        filteredBins.clear()
        filteredBins.addAll(allBins)

        adapter.notifyDataSetChanged()

        lastCameraMoveToken++
        updateMapMarkers(filteredBins, lastCameraMoveToken)
    }


    // -----------------------------
    // Apply Filter (kept for potential future use)
    // -----------------------------
    private fun applyFilter(type: String) {
        filteredBins.clear()

        if (type == "ALL") {
            filteredBins.addAll(allBins)
        } else {
            filteredBins.addAll(
                allBins.filter { it.binType.equals(type, true) }
            )
        }

        adapter.notifyDataSetChanged()

        lastCameraMoveToken++
        updateMapMarkers(filteredBins, lastCameraMoveToken)
    }

    // -----------------------------
    // Map Markers
    // -----------------------------
    private fun updateMapMarkers(list: List<DropOffLocation>, cameraToken: Int) {
        if (!isMapReady) {
            pendingBins = list
            return
        }

        googleMap.clear()

        val limited = list.take(40)  // show only 40 nearest bins

        limited.forEach { bin ->
            googleMap.addMarker(
                MarkerOptions()
                    .position(LatLng(bin.latitude, bin.longitude))
                    .title(bin.name)
                    .snippet(bin.address)
            )
        }

        if (limited.isNotEmpty()) {
            val nearest = limited.first()
            googleMap.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(nearest.latitude, nearest.longitude),
                    14f
                )
            )
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
                fetchBinsWithLocation("")
            }

            chipBlueBin.setOnClickListener {
                chipBlueBin.isChecked = true
                fetchBinsWithLocation("BLUEBIN")
            }

            chipEwaste.setOnClickListener {
                chipEwaste.isChecked = true
                fetchBinsWithLocation("EWASTE")
            }

            chipLighting.setOnClickListener {
                chipLighting.isChecked = true
                fetchBinsWithLocation("Lighting")
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

