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
import iss.nus.edu.sg.webviews.binitrightmobileapp.network.RetrofitClient
import kotlinx.coroutines.launch

class NearByBinFragment : Fragment(R.layout.fragment_near_by_bin), OnMapReadyCallback {

    private var _binding: FragmentNearByBinBinding? = null
    private val binding get() = _binding!!

    private lateinit var googleMap: GoogleMap
    private lateinit var locationClient: FusedLocationProviderClient

    private val nearbyBins = mutableListOf<DropOffLocation>()
    private val displayedBins = mutableListOf<DropOffLocation>()
    private var adapter: NearByBinsAdapter? = null

    private var selectedBinType: String? = null
    private var wasteCategory: String? = null
    private var mappedWasteCategory: String? = null

    private var pendingBins: List<DropOffLocation>? = null
    private var isMapReady = false
    private var hasZoomedToBins = false
    private var hasFetchedBins = false
    private var lastQueryLat: Double = FALLBACK_LAT
    private var lastQueryLng: Double = FALLBACK_LNG

    companion object {
        private const val TAG = "NearByBinFragment"
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val SEARCH_RADIUS_METERS = 10000
        private const val MAX_DISPLAY_BINS = 3
        private const val FALLBACK_LAT = 1.2921
        private const val FALLBACK_LNG = 103.77
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentNearByBinBinding.bind(view)

        locationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        retrieveScanArguments()
        updateFlowHint()
        setupMap()
        setupRecyclerView()
    }

    private fun retrieveScanArguments() {
        arguments?.let { bundle ->
            val receivedBinType = bundle.getString("selectedBinType") ?: bundle.getString("binType")
            selectedBinType = ScannedCategoryHelper.normalizeBinType(receivedBinType)

            wasteCategory = bundle.getString("wasteCategory") ?: bundle.getString("scannedCategory")
            mappedWasteCategory = bundle.getString("mappedWasteCategory")

            if (selectedBinType.isNullOrBlank()) {
                val inferredRecyclable = ScannedCategoryHelper.isLikelyRecyclableCategory(wasteCategory)
                selectedBinType = ScannedCategoryHelper.toBinType(wasteCategory, inferredRecyclable)
                    .takeIf { it.isNotBlank() }
            }

            if (mappedWasteCategory.isNullOrBlank()) {
                val fromCategory = ScannedCategoryHelper.toCheckInWasteType(wasteCategory)
                mappedWasteCategory = if (fromCategory != "Others") {
                    fromCategory
                } else {
                    ScannedCategoryHelper.toCheckInWasteTypeFromBinType(selectedBinType)
                }
            }

            Log.d(TAG, "Received selectedBinType=$selectedBinType wasteCategory=$wasteCategory mapped=$mappedWasteCategory")
        }
    }

    private fun updateFlowHint() {
        val hint = when (selectedBinType) {
            "BLUEBIN" -> getString(R.string.nearby_hint_blue_bins)
            "EWASTE" -> getString(R.string.nearby_hint_ewaste_bins)
            "LIGHTING" -> getString(R.string.nearby_hint_lighting_bins)
            else -> getString(R.string.nearby_hint_default_bins)
        }
        binding.tvFlowHint.text = hint
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
        adapter = NearByBinsAdapter(displayedBins) { selectedBin ->
            navigateToCheckIn(selectedBin)
        }
        binding.binsRecyclerView.adapter = adapter
    }

    private fun applyFlowFilter() {
        applyFlowFilter(true)
    }

    private fun applyFlowFilter(useFlowFilter: Boolean) {
        val oldSize = displayedBins.size
        displayedBins.clear()
        displayedBins.addAll(
            (if (useFlowFilter) {
                filterByFlowBinType(nearbyBins, selectedBinType)
            } else {
                nearbyBins
            })
                .take(MAX_DISPLAY_BINS)
        )

        adapter?.notifyForDataChange(oldSize, displayedBins.size)
        updateMapMarkers(displayedBins)

        Log.d(
            TAG,
            "Flow list updated with selectedBinType=$selectedBinType appliedFlowFilter=$useFlowFilter displayed=${displayedBins.size} total=${nearbyBins.size} limit=$MAX_DISPLAY_BINS"
        )
    }

    private fun navigateToCheckIn(bin: DropOffLocation) {
        val resolvedMappedCategory = mappedWasteCategory
            ?.takeIf { it.isNotBlank() }
            ?: run {
                val fromCategory = ScannedCategoryHelper.toCheckInWasteType(wasteCategory)
                if (fromCategory != "Others") {
                    fromCategory
                } else {
                    ScannedCategoryHelper.toCheckInWasteTypeFromBinType(bin.binType)
                }
            }

        val resolvedSelectedBinType = ScannedCategoryHelper.normalizeBinType(bin.binType)
            .ifBlank { selectedBinType.orEmpty() }

        val bundle = Bundle().apply {
            putString("binId", bin.id)
            putString("binName", bin.name)
            putString("binAddress", bin.address)
            putString("binType", bin.binType)
            putDouble("binLatitude", bin.latitude)
            putDouble("binLongitude", bin.longitude)

            putString("selectedBinType", resolvedSelectedBinType)
            wasteCategory?.let { putString("wasteCategory", it) }
            putString("mappedWasteCategory", resolvedMappedCategory)
        }

        findNavController().navigate(
            R.id.action_nearByBinFragment_to_checkInFragment,
            bundle
        )
    }

    private fun fetchUserLocation() {
        if (hasFetchedBins) {
            return
        }

        if (
            ActivityCompat.checkSelfPermission(
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
                if (hasFetchedBins) {
                    return@addOnSuccessListener
                }

                val lat = location?.latitude ?: FALLBACK_LAT
                val lng = location?.longitude ?: FALLBACK_LNG
                hasFetchedBins = true
                fetchNearbyBins(lat, lng)
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "Failed to get location: ${error.message}", error)
                hasFetchedBins = true
                fetchNearbyBins(FALLBACK_LAT, FALLBACK_LNG)
            }
    }

    private fun fetchNearbyBins(lat: Double, lng: Double) {
        lastQueryLat = lat
        lastQueryLng = lng

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val flowBinType = selectedBinType?.takeIf { it.isNotBlank() }
                Log.d(TAG, "Fetching nearby bins with flowBinType=$flowBinType lat=$lat lng=$lng")
                val api = RetrofitClient.apiService()
                val requestedBins = api.getNearbyBins(lat, lng, SEARCH_RADIUS_METERS, flowBinType)
                val filteredRequestedBins = filterByFlowBinType(requestedBins, flowBinType)
                var shouldApplyFlowFilter = true
                val effectiveBins = if (!flowBinType.isNullOrBlank() && filteredRequestedBins.isNotEmpty() && filteredRequestedBins.size < MAX_DISPLAY_BINS) {
                    val expandedTypeBins = filterByFlowBinType(
                        api.getNearbyBins(lat, lng, null, flowBinType),
                        flowBinType
                    )
                    val mergedBins = (filteredRequestedBins + expandedTypeBins).distinctBy { it.id }
                    if (mergedBins.size > filteredRequestedBins.size) {
                        Log.d(
                            TAG,
                            "Top-up results for $flowBinType: strict=${filteredRequestedBins.size} merged=${mergedBins.size}"
                        )
                        updateExpandedTypeHint(flowBinType)
                    }
                    mergedBins
                } else if (filteredRequestedBins.isEmpty() && !flowBinType.isNullOrBlank()) {
                    Log.w(TAG, "No bins for strict flowBinType=$flowBinType; retrying with expanded type search")
                    val expandedTypeBins = filterByFlowBinType(
                        api.getNearbyBins(lat, lng, null, flowBinType),
                        flowBinType
                    )

                    if (expandedTypeBins.isNotEmpty()) {
                        Log.w(TAG, "Found ${expandedTypeBins.size} bins for expanded type search: $flowBinType")
                        updateExpandedTypeHint(flowBinType)
                        expandedTypeBins
                    } else {
                        Log.w(TAG, "No bins for expanded flowBinType=$flowBinType; retrying without server binType filter")
                        val fallbackBins = api.getNearbyBins(lat, lng, SEARCH_RADIUS_METERS, null)
                        val filteredFallbackBins = filterByFlowBinType(fallbackBins, flowBinType)

                        if (filteredFallbackBins.isNotEmpty()) {
                            filteredFallbackBins
                        } else {
                            shouldApplyFlowFilter = false
                            if (fallbackBins.isNotEmpty()) {
                                Log.w(TAG, "No strict $flowBinType bins found; showing nearest available bins instead")
                                binding.tvFlowHint.text = getString(R.string.nearby_hint_fallback_bins)
                            } else {
                                Log.w(TAG, "No bins found in $SEARCH_RADIUS_METERS m")
                            }
                            fallbackBins
                        }
                    }
                } else {
                    filteredRequestedBins
                }
                nearbyBins.clear()
                nearbyBins.addAll(effectiveBins)
                applyFlowFilter(shouldApplyFlowFilter)
            } catch (error: Exception) {
                Log.e(TAG, "Retrofit error: ${error.message}", error)
            }
        }
    }

    private fun filterByFlowBinType(
        bins: List<DropOffLocation>,
        flowBinType: String?
    ): List<DropOffLocation> {
        if (flowBinType.isNullOrBlank()) {
            return bins
        }
        return bins.filter { bin ->
            ScannedCategoryHelper.normalizeBinType(bin.binType) == flowBinType
        }
    }

    private fun updateExpandedTypeHint(flowBinType: String) {
        val hint = when (flowBinType) {
            "EWASTE" -> getString(R.string.nearby_hint_ewaste_bins_expanded)
            "LIGHTING" -> getString(R.string.nearby_hint_lighting_bins_expanded)
            "BLUEBIN" -> getString(R.string.nearby_hint_blue_bins_expanded)
            else -> getString(R.string.nearby_hint_default_bins)
        }
        binding.tvFlowHint.text = hint
    }

    private fun updateMapMarkers(bins: List<DropOffLocation>) {
        if (!isMapReady) {
            pendingBins = bins
            return
        }

        googleMap.clear()
        if (bins.isEmpty()) {
            googleMap.moveCamera(
                CameraUpdateFactory.newLatLngZoom(LatLng(lastQueryLat, lastQueryLng), 13f)
            )
            return
        }

        bins.forEach { bin ->
            val position = LatLng(bin.latitude, bin.longitude)
            val distanceText = if (bin.distanceMeters > 0) {
                getString(R.string.distance_meter_away, bin.distanceMeters)
            } else {
                getString(R.string.nearby_distance_unknown)
            }

            googleMap.addMarker(
                MarkerOptions()
                    .position(position)
                    .title(bin.name)
                    .snippet("${bin.binType} - $distanceText")
            )
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
            hasFetchedBins = true
            fetchNearbyBins(FALLBACK_LAT, FALLBACK_LNG)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

