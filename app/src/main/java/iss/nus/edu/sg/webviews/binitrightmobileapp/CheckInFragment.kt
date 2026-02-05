package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import iss.nus.edu.sg.webviews.binitrightmobileapp.databinding.FragmentCheckInBinding
import iss.nus.edu.sg.webviews.binitrightmobileapp.CheckInData
import iss.nus.edu.sg.webviews.binitrightmobileapp.network.RetrofitClient
import iss.nus.edu.sg.webviews.binitrightmobileapp.utils.JwtUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CheckInFragment : Fragment() {

    private val userId: Long by lazy {
        val prefs = requireContext().getSharedPreferences("APP_PREFS", Context.MODE_PRIVATE)
        val token = prefs.getString("JWT_TOKEN", null)
        if (token != null) {
            JwtUtils.getUserIdFromToken(token) ?: -1L
        } else {
            -1L
        }
    }
    private val radius = 100000.0 // meters

    private var binId: Long = -1
    private var binName: String = ""
    private var binAddress: String = ""
    private var binType: String = ""
    private var binLatitude: Double = 0.0
    private var binLongitude: Double = 0.0
    private var selectedBinType: String? = null
    private var wasteCategory: String? = null
    private var currentCount = 0

    private var _binding: FragmentCheckInBinding? = null
    private val binding get() = _binding!!

    private var recordedFile: File? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    companion object {
        private const val TAG = "CheckInFragment"
    }

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                checkLocationAndNavigate()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Location permission is required to check in.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCheckInBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recordedFile = null
        retrieveBinInformation()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        updateSubmitButtonState(false)
        retrieveScannedBinType()
        setupButtons()
        setupCounter()
        setupChipListeners()
        displayBinInformation()

        Log.d(TAG, "Current User ID: $userId")
    }

    private fun retrieveBinInformation() {
        arguments?.let { bundle ->
            binId = bundle.getLong("binId", -1)
            binName = bundle.getString("binName", "") ?: ""
            binAddress = bundle.getString("binAddress", "") ?: ""
            binType = bundle.getString("binType", "") ?: ""
            binLatitude = bundle.getDouble("binLatitude", 0.0)
            binLongitude = bundle.getDouble("binLongitude", 0.0)
            selectedBinType = bundle.getString("selectedBinType", "")?:""

        } ?: run {
            binId = 1
            binName = "Default Bin"
            binType = "BLUEBIN"
            binLatitude = 1.4689
            binLongitude = 103.8143
        }
    }

    private fun displayBinInformation() {
        binding.apply {
            tvLocationName.text = binName.ifEmpty { "Recycling Bin" }
            tvLocationAddress.text = binAddress.ifEmpty { "Location not available" }

            val itemTypeText = formatBinType(binType)
            etItemName.setText(itemTypeText)
        }
    }

    private fun formatBinType(type: String): String {
        return when (type) {
            "BlueBin" -> "BlueBin"
            "EWaste" -> "Electronic Waste"
            "Lamp" -> "Lighting"
            else -> ""
        }
    }
    private fun setupButtons() {
        binding.btnBackHome.setOnClickListener {
            findNavController().navigate(R.id.action_checkInFragment_to_homeFragment)
        }

        binding.btnBackToHome.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnRecordVideo.setOnClickListener {
            requestPermissionNeeded()
        }

        binding.btnSubmit.setOnClickListener {
            uploadVideo()
        }
    }

    private fun setupCounter() {
        binding.btnIncrease.setOnClickListener {
            currentCount++
            updateCounterDisplay()
        }

        binding.btnDecrease.setOnClickListener {
            if (currentCount > 0) {
                currentCount--
                updateCounterDisplay()
            }
        }

        findNavController()
            .currentBackStackEntry
            ?.savedStateHandle
            ?.getLiveData<Int>("item_count")
            ?.observe(viewLifecycleOwner) {
                currentCount = it
                updateCounterDisplay()
            }
    }

    private fun updateCounterDisplay() {
        binding.tvItemCount.text = currentCount.toString()
    }

    private fun setupChipListeners() {
        binding.cgItemType.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isEmpty()) {
                return@setOnCheckedStateChangeListener
            }

            val checkedId = checkedIds.first()
            val selectedText = when (checkedId) {
                R.id.chipPlastic -> "Plastic"
                R.id.chipPaper -> "Paper"
                R.id.chipGlass -> "Glass"
                R.id.chipMetal -> "Metal"
                R.id.chipEWaste -> "E-Waste"
                R.id.chipLighting -> "Lighting"
                else -> {
                    ""
                }
            }

            binding.etItemName.setText(selectedText)
        }
    }

    private fun updateSubmitButtonState(enabled: Boolean) {
        binding.btnSubmit.apply {
            isEnabled = enabled

            if (enabled) {
                setBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(),
                        android.R.color.holo_green_dark
                    )
                )
                alpha = 1.0f
            } else {
                setBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(),
                        android.R.color.darker_gray
                    )
                )
                alpha = 0.5f
            }
        }
    }

    fun requestPermissionNeeded() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                checkLocationAndNavigate()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                Toast.makeText(requireContext(), "Location access is needed.", Toast.LENGTH_SHORT).show()
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            else -> {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    fun checkLocationAndNavigate() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(requireContext(), "Location permission required.", Toast.LENGTH_SHORT).show()
            return
        }

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location == null) {
                    Toast.makeText(requireContext(), "Unable to detect location.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val isWithinRange = LocationChecker.isWithinRadius(
                    location.latitude, location.longitude, binLatitude, binLongitude, radius
                )

                if (isWithinRange) {
                    val currentValue = binding.tvItemCount.text.toString().toIntOrNull() ?: 0
                    findNavController().currentBackStackEntry?.savedStateHandle?.set("item_count", currentValue)
                    findNavController().navigate(R.id.action_checkIn_to_videoRecord)
                } else {
                    Toast.makeText(requireContext(), "Must be near the bin.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun loadLastRecordedVideo() {
        val videoDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: run {
            updateSubmitButtonState(false)
            return
        }

        val lastVideo = videoDir.listFiles { file -> file.extension == "mp4" }?.maxByOrNull { it.lastModified() }

        if (lastVideo != null) {
            recordedFile = lastVideo
            updateSubmitButtonState(true)
            Log.d(TAG, "Video loaded: ${lastVideo.name}")
        } else {
            updateSubmitButtonState(false)
            Log.d(TAG, "No video found")
        }
    }

    override fun onResume() {
        super.onResume()
        val returnedFromRecording = findNavController().currentBackStackEntry?.savedStateHandle?.contains("item_count") == true
        if (returnedFromRecording) {
            loadLastRecordedVideo()
        } else {
            updateSubmitButtonState(false)
        }
    }

    private fun retrieveScannedBinType() {
        arguments?.let { bundle ->
            selectedBinType = bundle.getString("selectedBinType")
        }
    }

    private fun uploadVideo() {
        val file = recordedFile ?: run {
            Toast.makeText(requireContext(), "No Video to upload", Toast.LENGTH_SHORT).show()
            return
        }

        if (userId == -1L) {
            showStatus("Error: User not logged in. Please Login first!", isError = true)
            return
        }

        val durationSeconds = getVideoDurationSeconds(file)
        var wasteType = binding.etItemName.text.toString().trim()
        if (wasteType.isEmpty()) wasteType = "Paper"

        val currentTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date())

        Log.d(TAG, "Submitting: User=$userId, Bin=$binId, Cat=$wasteType, Time=$currentTime")

        val checkInData = CheckInData(
            userId = userId,
            duration = durationSeconds.toLong(),
            binId = binId,
            wasteCategory = wasteCategory ?: wasteType,
            quantity = currentCount,
            videoKey = file.name,
            checkInTime = currentTime
        )

        lifecycleScope.launch {
            try {
                updateSubmitButtonState(false)

                val response = RetrofitClient.instance.submitRecycleCheckIn(checkInData)

                if (response.isSuccessful) {
                    val resBody = response.body()
                    if (resBody != null && resBody.responseCode == "SUCCESS") {
                        val message = "SUCCESS\n${resBody.responseDesc}"
                        showStatus(message, isError = false)

                        checkAndUnlockAchievements()
                        delay(1200)
                        findNavController().popBackStack()
                    } else {
                        showStatus("Failed: ${resBody?.responseDesc}", isError = true)
                        updateSubmitButtonState(true)
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "Server Error: $errorBody")
                    showStatus("Server Error (${response.code()})", isError = true)
                    updateSubmitButtonState(true)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Network error", e)
                showStatus("Network Error: ${e.message}", isError = true)
                updateSubmitButtonState(true)
            }
        }
    }

    private fun checkAndUnlockAchievements() {
        lifecycleScope.launch {
            try {
                RetrofitClient.instance.unlockAchievement(userId, 1L)

                val historyRes = RetrofitClient.instance.getRecycleHistory()
                if (historyRes.isSuccessful && historyRes.body() != null) {
                    val historyList = historyRes.body()!!
                    if (historyList.size >= 10) {
                        RetrofitClient.instance.unlockAchievement(userId, 2L)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Achievement check failed", e)
            }
        }
    }

    private fun getVideoDurationSeconds(file: File): Int {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            (time?.toLong() ?: 0L).toInt() / 1000
        } catch (e: Exception) {
            0
        }
    }

    private fun showStatus(message: String, isError: Boolean) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}