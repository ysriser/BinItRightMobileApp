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
import com.google.gson.Gson
import iss.nus.edu.sg.webviews.binitrightmobileapp.databinding.FragmentCheckInBinding
import iss.nus.edu.sg.webviews.binitrightmobileapp.network.RetrofitClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class CheckInFragment : Fragment() {

    private val userId: Long by lazy {
        val prefs = requireContext().getSharedPreferences("APP_PREFS", Context.MODE_PRIVATE)
        prefs.getLong("USER_ID", -1L)
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
            binId = 3
            binName = "Paper"
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
            if (currentCount > 0) { // Prevent going below 0
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
                R.id.chipEWaste -> "Electronic Waste"
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

            shouldShowRequestPermissionRationale(
                Manifest.permission.ACCESS_FINE_LOCATION
            ) -> {
                Toast.makeText(
                    requireContext(),
                    "Location access is needed to verify your recycling location.",
                    Toast.LENGTH_SHORT
                ).show()

                locationPermissionLauncher.launch(
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            }

            else -> {
                locationPermissionLauncher.launch(
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            }
        }
    }

    fun checkLocationAndNavigate() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(
                requireContext(),
                "Location permission is required to check in.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            null
        ).addOnSuccessListener { location ->
            if (location == null) {
                Toast.makeText(
                    requireContext(),
                    "Unable to detect your location. Please try again.",
                    Toast.LENGTH_SHORT
                ).show()
                return@addOnSuccessListener
            }

            val isWithinRange = LocationChecker.isWithinRadius(
                location.latitude,
                location.longitude,
                binLatitude,
                binLongitude,
                radius
            )

            if (isWithinRange) {
                val currentValue = binding.tvItemCount.text.toString().toIntOrNull() ?: 0
                findNavController()
                    .currentBackStackEntry
                    ?.savedStateHandle
                    ?.set("item_count", currentValue)

                findNavController().navigate(R.id.action_checkIn_to_videoRecord)
            } else {
                Toast.makeText(
                    requireContext(),
                    "You must be near the recycling bin to record a video.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun loadLastRecordedVideo() {
        val videoDir =
            requireContext().getExternalFilesDir(Environment.DIRECTORY_MOVIES)
                ?: run {
                    updateSubmitButtonState(false)
                    return
                }

        val lastVideo = videoDir
            .listFiles { file -> file.extension == "mp4" }
            ?.maxByOrNull { it.lastModified() }

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

        val returnedFromRecording = findNavController()
            .currentBackStackEntry
            ?.savedStateHandle
            ?.contains("item_count") == true

        if (returnedFromRecording) {
            Log.d(TAG, "onResume - returned from recording, checking for video")
            loadLastRecordedVideo()
        } else {
            Log.d(TAG, "onResume - fresh entry, keeping button disabled")
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
            Toast.makeText(
                requireContext(),
                "No Video to upload",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val durationSeconds = getVideoDurationSeconds(file)
        val wasteType = binding.etItemName.text.toString()

        // 对齐 Master 的 CheckInData 结构: userId(Long), duration(Long), binId(Long), videoKey(String?)
        val checkInData = CheckInData(
            userId = userId,
            duration = durationSeconds.toLong(),
            binId = binId,
            wasteCategory = wasteCategory?: wasteType,
            quantity = currentCount,
            videoKey = null
        )

        val gson = Gson()
        val metadataJson = gson.toJson(checkInData)

        val metadataBody = metadataJson.toRequestBody("application/json".toMediaType())

        val videoBody = file.asRequestBody("video/mp4".toMediaType())

        val videoPart = MultipartBody.Part.createFormData(
            name = "video",
            filename = file.name,
            body = videoBody
        )

        lifecycleScope.launch {
            try {
                updateSubmitButtonState(false)

                val response = RetrofitClient.instance.submitRecycleCheckIn(
                    video = videoPart,
                    metadata = metadataBody
                )

                if (response.isSuccessful) {
                    val resBody = response.body()
                    if (resBody != null) {
                        val message = resBody.responseCode + "\n" + resBody.responseDesc
                        showStatus(message, isError = false)

                        if (resBody.responseCode == "0000") {

                            checkAndUnlockAchievements()
                            
                            delay(1200) // allow user to read success message
                            findNavController().popBackStack()
                        } else {
                            updateSubmitButtonState(true)
                        }
                    } else {
                        showStatus("Empty server response", isError = true)
                        updateSubmitButtonState(true)
                    }
                } else {
                    showStatus(
                        "Submission failed (${response.code()})",
                        isError = true
                    )
                    updateSubmitButtonState(true)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Upload error", e)
                showStatus("An error occurred: ${e.message}", isError = true)
                updateSubmitButtonState(true)
            }
        }
    }

    private fun checkAndUnlockAchievements() {
        lifecycleScope.launch {
            try {
                RetrofitClient.instance.unlockAchievement(userId, 1L)
            } catch (e: Exception) {
                Log.e(TAG, "Achievement unlock failed", e)
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