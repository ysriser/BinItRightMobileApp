package iss.nus.edu.sg.todo.samplebin


import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log.e
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import iss.nus.edu.sg.todo.samplebin.model.DropOffLocation
import iss.nus.edu.sg.todo.samplebin.databinding.ActivityFindRecyclingBinBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL

class FindRecyclingBinActivity : AppCompatActivity(),
    OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private var pendingBins: List<DropOffLocation>? = null
    private var isMapReady = false
    private var lastCameraMoveToken = 0
    private val allBins = mutableListOf<DropOffLocation>()
    private val filteredBins = mutableListOf<DropOffLocation>()
    private lateinit var adapter: FindBinsAdapter
    private lateinit var binding: ActivityFindRecyclingBinBinding
    private lateinit var locationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityFindRecyclingBinBinding.inflate(layoutInflater)
        setContentView(binding.root)

        locationClient = LocationServices.getFusedLocationProviderClient(this)

        setupToolbar()
        setupMap()
        setupRecyclerView()
        setupChipFiltering()

    }

    // -----------------------------
    // Toolbar
    // -----------------------------
    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
    }

    // -----------------------------
    // Map
    // -----------------------------
    private fun setupMap() {
        val mapFragment =
            supportFragmentManager.findFragmentById(binding.mapContainer.id)
                    as? SupportMapFragment
                ?: SupportMapFragment.newInstance().also {
                    supportFragmentManager.beginTransaction()
                        .replace(binding.mapContainer.id, it)
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
        fetchBinsWithLocation("")
    }

    // -----------------------------
    // RecyclerView
    // -----------------------------
    private fun setupRecyclerView() {
        binding.binsRecyclerView.layoutManager = LinearLayoutManager(this)

        adapter = FindBinsAdapter(filteredBins)
        binding.binsRecyclerView.adapter = adapter
    }



    // -----------------------------
// Fetch All Bins From Backend
// -----------------------------
    private fun fetchBinsWithLocation(binType: String) {

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1001
            )
            return
        }

        locationClient.lastLocation.addOnSuccessListener { location ->

            val userLat = location?.latitude ?: 1.28797431732068
            val userLng = location?.longitude ?: 103.805808773107998


            lifecycleScope.launch {

                val json = withContext(Dispatchers.IO) {

               //     val url = URL("http://10.0.2.2:8081/api/bins/all?lat=$userLat&lng=$userLng&binType=$binType")
                    val url = URL("http://192.168.1.37:8081/api/bins/all?lat=$userLat&lng=$userLng&binType=$binType")
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
               // Log.d("FIND_BINS_API", "Response JSON = $json")
                parseAllBinsJson(json)
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
                val obj = jsonArray.getJSONObject(i)

                tempList.add(
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
                        distanceMeters = obj.optDouble("distanceMeters", 0.0)
                    )
                )
            }

            allBins.clear()
            allBins.addAll(tempList)

            updateUI()
        }catch(e: Exception){
            e("FIND_BINS_PARSE_ERROR", "JSON Parse Error: ${e.message}")
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
            )
        }

     //   moveCamera(list, cameraToken)

        if (limited.isNotEmpty()) {
            val nearest = limited.first()
            googleMap.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(nearest.latitude, nearest.longitude),
                    16f
                )
            )
        }
    }




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
                fetchBinsWithLocation("LAMP")
            }
        }

    }

}

