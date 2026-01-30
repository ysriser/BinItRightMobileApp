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
import kotlin.collections.first
import kotlin.collections.isNotEmpty
import android.Manifest
import android.content.pm.PackageManager
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
    private var adapter: NearByBinsAdapter?= null
    private lateinit var locationClient: FusedLocationProviderClient

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentNearByBinBinding.bind(view)
        locationClient =
            LocationServices.getFusedLocationProviderClient(requireActivity())

        setupMap()
        setupRecyclerView()

    }

    // -----------------------------
    // Map
    // -----------------------------
    private fun setupMap() {

        val mapFragment = childFragmentManager.findFragmentById(binding.mapContainer.id)
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
        //googleMap.uiSettings.isMyLocationButtonEnabled = true

        fetchUserLocation()
    }


    // -----------------------------
    // RecyclerView
    // -----------------------------
    private fun setupRecyclerView() {

        binding.binsRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = NearByBinsAdapter(nearbyBins) { selectedBin ->
            // Navigate to check-in fragment with selected bin data
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

        findNavController().navigate(
            R.id.action_nearByBinFragment_to_checkInFragment,
            bundle
        )
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
                1001
            )
            return
        }

        locationClient.lastLocation
            .addOnSuccessListener { location ->

                if (hasFetchedBins) return@addOnSuccessListener

                if (location != null) {
                    // Normal case
                    fetchNearbyBins(location.latitude, location.longitude)
                } else {
                    // FALLBACK
                    val fallbackLat = 1.3521
                    val fallbackLng = 103.8198

                    hasFetchedBins = true
                    fetchNearbyBins(fallbackLat, fallbackLng)
                }
            }
    }

    // -----------------------------
    // Nearby Bins based on user location
    // -----------------------------
    private fun fetchNearbyBins(lat: Double, lng: Double) {
        viewLifecycleOwner.lifecycleScope.launch {

            val json = withContext(Dispatchers.IO) {
//                val url = java.net.URL(
//                    "http://10.0.2.2:8081/api/bins/nearby?lat=$lat&lng=$lng&radius=3000"
//                )

                val url = java.net.URL(
                    "http://192.168.88.3:8081/api/bins/nearby?lat=$lat&lng=$lng&radius=3000"
                )

                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val stream = if (connection.responseCode in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }

                BufferedReader(InputStreamReader(stream)).use { it.readText() }
            }

            parseBinsJson(json)
        }
    }

    // -----------------------------
    // Parse Response
    // -----------------------------
    private fun parseBinsJson(json: String) {
        val jsonArray = org.json.JSONArray(json)
        val parsedBins = mutableListOf<DropOffLocation>()


        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)

            parsedBins.add(
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
                    distanceMeters = obj.getDouble("distanceMeters")
                )
            )
        }

        if (!isAdded) return

        nearbyBins.clear()
        nearbyBins.addAll(parsedBins)
        adapter?.notifyDataSetChanged()
        updateMapMarkers(parsedBins)

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
                    .snippet("${bin.distanceMeters} m")
            )
        }

        // Move camera after markers are rendered
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

        if (requestCode == 1001 &&
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