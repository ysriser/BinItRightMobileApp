package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
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
import iss.nus.edu.sg.webviews.binitrightmobileapp.network.RetrofitClient
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
    private val filteredBins = mutableListOf<DropOffLocation>()
    private var adapter: NearByBinsAdapter? = null
    private var selectedBinType: String? = null
    private var wasteCategory: String? = null
    private var currentFilterBinType: String = "All"
    private var pendingBins: List<DropOffLocation>? = null
    private var isMapReady = false
    private var hasZoomedToBins = false
    private var hasFetchedBins = false

    companion object {
        private const val TAG = "NearByBinFragment"
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val FALLBACK_LAT = 1.2921
        private const val FALLBACK_LNG = 103.77
        private val BIN_TYPES = listOf("All", "BlueBin", "EWaste", "Lighting")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentNearByBinBinding.bind(view)

        locationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        retrieveScanArguments()
        setupBinTypeSpinner()
        setupMap()
        setupRecyclerView()
    }

    private fun retrieveScanArguments() {
        arguments?.let { bundle ->
            selectedBinType = bundle.getString("selectedBinType") ?: bundle.getString("binType")
            wasteCategory = bundle.getString("wasteCategory") ?: bundle.getString("scannedCategory")

            Log.d(TAG, "### Received selectedBinType: $selectedBinType")
            Log.d(TAG, "### Received wasteCategory: $wasteCategory")
        }
    }

    private fun setupBinTypeSpinner() {
        val spinnerAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            BIN_TYPES
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        binding.spinnerBinType.adapter = spinnerAdapter

        // If selectedBinType matches one of the valid types, set it as default
        val defaultPosition = when (selectedBinType) {
            "BlueBin", "EWaste", "Lighting" -> BIN_TYPES.indexOf(selectedBinType)
            else -> 0 // "All" by default
        }
        binding.spinnerBinType.setSelection(defaultPosition)
        currentFilterBinType = BIN_TYPES[defaultPosition]

        binding.spinnerBinType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentFilterBinType = BIN_TYPES[position]
                Log.d(TAG, "### Filter changed to: $currentFilterBinType")
                filterBins()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }
    }

    private fun filterBins() {
        filteredBins.clear()

        if (currentFilterBinType == "All") {
            filteredBins.addAll(nearbyBins)
        } else {
            // Filter based on binType matching the selected filter
            filteredBins.addAll(nearbyBins.filter { bin ->
                bin.binType.equals(currentFilterBinType, ignoreCase = true)
            })
        }

        Log.d(TAG, "### Total bins: ${nearbyBins.size}")
        Log.d(TAG, "### Filtered ${filteredBins.size} bins for type: $currentFilterBinType")

        // Log the bins to debug
        nearbyBins.forEach { bin ->
            Log.d(TAG, "### Bin: ${bin.name}, Type: ${bin.binType}")
        }

        // Notify adapter that data has changed
        adapter?.notifyDataSetChanged()

        // Update map with filtered bins
        updateMapMarkers(filteredBins)
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
        // Pass filteredBins to the adapter
        adapter = NearByBinsAdapter(filteredBins) { selectedBin ->
            navigateToCheckIn(selectedBin)
        }
        binding.binsRecyclerView.adapter = adapter
    }

    private fun navigateToCheckIn(bin: DropOffLocation) {
        Log.d(TAG, "### Navigating to CheckIn with wasteCategory: $wasteCategory")

        val bundle = Bundle().apply {
            putString("binId", bin.id)
            putString("binName", bin.name)
            putString("binAddress", bin.address)
            putString("binType", bin.binType)
            putDouble("binLatitude", bin.latitude)
            putDouble("binLongitude", bin.longitude)

            // Pass both fields forward
            selectedBinType?.let {
                putString("selectedBinType", it)
                Log.d(TAG, "### Passing selectedBinType: $it")
            }
            wasteCategory?.let {
                putString("wasteCategory", it)
                Log.d(TAG, "### Passing wasteCategory: $it")
            }
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
                //fetchNearbyBins(1.2921, 103.77)
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
                Log.d(TAG, "Fetching bins via Retrofit for lat=$lat, lng=$lng")

                // Retrofit does the background work and parsing for you
                val bins = RetrofitClient.apiService().getNearbyBins(lat, lng, 10000)

                Log.d(TAG, "Successfully fetched ${bins.size} bins")

                // Update UI on the main thread
                nearbyBins.clear()
                nearbyBins.addAll(bins)
                filterBins()

            } catch (e: Exception) {
                Log.e(TAG, "Retrofit Error: ${e.message}", e)
                // Handle error (e.g., show a Toast)
            }
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

