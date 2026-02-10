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

    companion object {
        private const val TAG = "NearByBinFragment"
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
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
            "BLUEBIN" -> "Showing nearest blue recycling bins"
            "EWASTE" -> "Showing nearest e-waste bins"
            "LIGHTING" -> "Showing nearest lighting bins"
            else -> "Showing nearest recycling bins"
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
        displayedBins.clear()
        displayedBins.addAll(nearbyBins)

        adapter?.notifyDataSetChanged()
        updateMapMarkers(displayedBins)

        Log.d(
            TAG,
            "Flow list updated without filtering. selectedBinType=$selectedBinType total=${nearbyBins.size}"
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
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val flowBinType = selectedBinType?.takeIf { it.isNotBlank() }
                Log.d(TAG, "Fetching nearby bins with flowBinType=$flowBinType lat=$lat lng=$lng")
                val bins = RetrofitClient.apiService().getNearbyBins(lat, lng, 10000, flowBinType)
                nearbyBins.clear()
                nearbyBins.addAll(bins)
                applyFlowFilter()
            } catch (error: Exception) {
                Log.e(TAG, "Retrofit error: ${error.message}", error)
            }
        }
    }

    private fun updateMapMarkers(bins: List<DropOffLocation>) {
        if (!isMapReady) {
            pendingBins = bins
            return
        }

        googleMap.clear()
        if (bins.isEmpty()) {
            return
        }

        bins.forEach { bin ->
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

