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
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.DropOffLocation
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
    private var scannedBinType: String? = null
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

        retrieveScannedBinType()
        setupMap()
        setupRecyclerView()
    }

    private fun retrieveScannedBinType() {
        arguments?.let { bundle ->
            scannedBinType = bundle.getString("binType")
            if (scannedBinType != null) {
            } else {
            }
        } ?: run {
            scannedBinType = null
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
        googleMap = map
        isMapReady = true

        googleMap.uiSettings.isZoomControlsEnabled = true

        pendingBins?.let {
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
    }

    private fun navigateToCheckIn(bin: DropOffLocation) {

        val bundle = Bundle().apply {
            putLong("binId", bin.id)
            putString("binName", bin.name)
            putString("binAddress", bin.address)
            putString("binType", bin.binType)
            putDouble("binLatitude", bin.latitude)
            putDouble("binLongitude", bin.longitude)
        }

        // Pass the scanned bin type if it exists
        scannedBinType?.let {
            bundle.putString("scannedBinType", it)
            Log.d(TAG, "  Added scannedBinType to bundle: $it")
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
        if (hasFetchedBins) return

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

                hasFetchedBins = true
                fetchNearbyBins(1.29, 103.78)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to get location", e)
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
                val json = withContext(Dispatchers.IO) {
//                    val url = java.net.URL(
//                        "http://10.0.2.2:8081/api/bins/nearby?lat=$lat&lng=$lng&radius=3000"
//                    )

                    val url = java.net.URL(
                        "http://192.168.88.4:8081/api/bins/nearby?lat=$lat&lng=$lng&radius=3000"
                    )

                    val connection = url.openConnection() as java.net.HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 10000
                    connection.readTimeout = 10000

                    try {
                        val stream = if (connection.responseCode in 200..299) {
                            connection.inputStream
                        } else {
                            connection.errorStream
                        }

                        BufferedReader(InputStreamReader(stream)).use { it.readText() }
                    } finally {
                        connection.disconnect()
                    }
                }

                Log.d(TAG, "Response JSON = $json")
                parseBinsJson(json)
            } catch (e: Exception) {
                // Handle error - maybe show a toast or error message
            }
        }
    }

    // -----------------------------
    // Parse Response
    // -----------------------------
    private fun parseBinsJson(json: String) {
        try {
            val parsedBins = BinJsonParser.parse(json)


            if (!isAdded){
                return
            }

            nearbyBins.clear()
            nearbyBins.addAll(parsedBins)

            adapter?.notifyDataSetChanged()
            updateMapMarkers(parsedBins)
        } catch (e: Exception) {
        }
    }


    // -----------------------------
    // Mark location in map
    // -----------------------------
    private fun updateMapMarkers(bins: List<DropOffLocation>) {

        if (!isMapReady) {

            pendingBins = bins
            return
        }

        googleMap.clear()

        bins.forEach { bin ->

            googleMap.addMarker(
                MarkerOptions()
                    .position(LatLng(bin.latitude, bin.longitude))
                    .title(bin.name)
                    .snippet("${bin.distanceMeters.toInt()} m")
            )
        }

        if (!hasZoomedToBins && bins.isNotEmpty()) {
            val first = bins.first()

            googleMap.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(first.latitude, first.longitude),
                    16f
                )
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
            fetchUserLocation()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
