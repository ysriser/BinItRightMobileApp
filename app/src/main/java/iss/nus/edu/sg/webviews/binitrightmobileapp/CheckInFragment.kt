package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
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
import iss.nus.edu.sg.webviews.binitrightmobileapp.network.RetrofitClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class CheckInFragment : Fragment() {

    private var userId: Long = -1L // Default value
    private val radius = 100000.0 // meters

    private var binId: String = ""
    private var binName: String = ""
    private var binAddress: String = ""
    private var wasteType: String = ""
    private var binType: String = ""
    private var binLatitude: Double = 0.0
    private var binLongitude: Double = 0.0
    private var selectedBinType: String? = null
    private var wasteCategory: String? = null
    private var currentCount = 0
    private var _binding: FragmentCheckInBinding? = null
    private val binding get() = _binding!!
    private var isSubmitted = false
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
        try {
            _binding = FragmentCheckInBinding.inflate(inflater, container, false)
            return binding.root
        } catch (e: Exception) {
            throw e
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        try {
            super.onViewCreated(view, savedInstanceState)

            // Retrieve userId from SharedPreferences
            val prefs = requireContext().getSharedPreferences("APP_PREFS", Context.MODE_PRIVATE)
            userId = prefs.getLong("USER_ID", -1L)

            // Clear any previous recorded video
            recordedFile = null

            // Retrieve bin information from arguments
            retrieveBinInformation()

            fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

            // Initialize submit button as disabled
            updateSubmitButtonState(false)

            setupButtons()

            setupCounter()

            setupChipListeners()

            retrieveScannedBinType()

            displayBinInformation()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun retrieveBinInformation() {

        arguments?.let { bundle ->
            binId = bundle.getString("binId", "")
            binName = bundle.getString("binName", "") ?: ""
            binAddress = bundle.getString("binAddress", "") ?: ""
            binType = bundle.getString("binType", "") ?: ""
            binLatitude = bundle.getDouble("binLatitude", 0.0)
            binLongitude = bundle.getDouble("binLongitude", 0.0)
            selectedBinType = bundle.getString("selectedBinType", "") ?: ""
            wasteCategory = bundle.getString("wasteCategory") ?: bundle.getString("scannedCategory")

        } ?: run {
            binId = ""
            binName = "Paper"
            binType = "BLUEBIN"
            binLatitude = 1.4689
            binLongitude = 103.8143
        }

        Log.d(TAG, "###bin:${binId}")
    }

    private fun displayBinInformation() {

        binding.apply {
            tvLocationName.text = binName.ifEmpty { "Recycling Bin" }
            tvLocationAddress.text = binAddress.ifEmpty { "Location not available" }
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

        // Use validation logic instead of direct upload
        binding.btnSubmit.setOnClickListener {
            disableRecordVideo()
            handleSubmitWithValidation()
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

        // Observe item count from VideoRecordFragment
        findNavController()
            .currentBackStackEntry
            ?.savedStateHandle
            ?.getLiveData<Int>("item_count")
            ?.observe(viewLifecycleOwner) {
                currentCount = it
                updateCounterDisplay()
            }
    }

    private fun setupChipListeners() {

        try {
            binding.cgItemType.setOnCheckedStateChangeListener { _, checkedIds ->
                if (checkedIds.isEmpty()) {
                    return@setOnCheckedStateChangeListener
                }

                val checkedId = checkedIds.first()
                wasteType = when (checkedId) {
                    R.id.chipPlastic -> WasteCategoryMapper.TYPE_PLASTIC
                    R.id.chipPaper -> WasteCategoryMapper.TYPE_PAPER
                    R.id.chipGlass -> WasteCategoryMapper.TYPE_GLASS
                    R.id.chipMetal -> WasteCategoryMapper.TYPE_METAL
                    R.id.chipEWaste -> WasteCategoryMapper.TYPE_EWASTE
                    R.id.chipLighting -> WasteCategoryMapper.TYPE_LIGHTING
                    R.id.chipOthers -> WasteCategoryMapper.TYPE_OTHERS
                    else -> ""
                }
            }

        } catch (_: Exception) {
        }
    }

    private fun updateSubmitButtonState(enabled: Boolean) {
        binding.btnSubmit.apply {
            isEnabled = enabled

            if (enabled) {
                // Change to enabled color (green)
                setBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(),
                        android.R.color.holo_green_dark
                    )
                )
                alpha = 1.0f
            } else {
                // Change to disabled color (gray)
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
                // Permission already granted
                checkLocationAndNavigate()
            }

            // Returns true if the user previously denied the permission
            shouldShowRequestPermissionRationale(
                Manifest.permission.ACCESS_FINE_LOCATION
            ) -> {
                Toast.makeText(
                    requireContext(),
                    "Location access is needed to verify your recycling location.",
                    Toast.LENGTH_SHORT
                ).show()

                // Ask the user for fine location permission
                locationPermissionLauncher.launch(
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            }

            else -> {
                // First-time request
                locationPermissionLauncher.launch(
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            }
        }
    }

    fun checkLocationAndNavigate() {
        // Permission Check
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

        // Get last known location
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

            // Radius validation if location found
            val isWithinRange = LocationChecker.isWithinRadius(
                location.latitude,
                location.longitude,
                binLatitude,
                binLongitude,
                radius
            )

            if (isWithinRange) {
                val currentValue = binding.tvItemCount.text.toString().toIntOrNull() ?: 0
                // Save check in data before navigating
                findNavController()
                    .currentBackStackEntry
                    ?.savedStateHandle
                    ?.set("item_count", currentValue)

                // Allow navigation to VideoRecordFragment
                findNavController().navigate(R.id.action_checkIn_to_videoRecord)
            } else {
                // Block navigation
                Toast.makeText(
                    requireContext(),
                    "You must be near the recycling bin to record a video.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun loadLastRecordedVideo() {

        if (isSubmitted) return

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

        if (isSubmitted) {
            // HARD LOCK UI after success
            updateSubmitButtonState(false)
            disableRecordVideo()
            return
        }
        // Only check for video if we just came back from recording
        // (the savedStateHandle will have item_count if we recorded)
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
            wasteCategory = bundle.getString("wasteCategory") ?: bundle.getString("scannedCategory")
        } ?: run {
            selectedBinType = null
            wasteCategory = null
        }

        applyDefaultWasteTypeSelection()
    }

    private fun applyDefaultWasteTypeSelection() {
        val mappedWasteType = if (!wasteCategory.isNullOrBlank()) {
            WasteCategoryMapper.mapCategoryToWasteType(wasteCategory)
        } else {
            mapBinTypeToWasteType(selectedBinType)
        }

        wasteType = mappedWasteType

        val chipId = when (mappedWasteType) {
            WasteCategoryMapper.TYPE_PLASTIC -> R.id.chipPlastic
            WasteCategoryMapper.TYPE_PAPER -> R.id.chipPaper
            WasteCategoryMapper.TYPE_GLASS -> R.id.chipGlass
            WasteCategoryMapper.TYPE_METAL -> R.id.chipMetal
            WasteCategoryMapper.TYPE_EWASTE -> R.id.chipEWaste
            WasteCategoryMapper.TYPE_LIGHTING -> R.id.chipLighting
            else -> R.id.chipOthers
        }

        binding.cgItemType.check(chipId)
    }

    private fun mapBinTypeToWasteType(binType: String?): String {
        return when (binType?.trim()?.uppercase()) {
            "EWASTE", "E-WASTE" -> WasteCategoryMapper.TYPE_EWASTE
            "LIGHTING", "LAMP" -> WasteCategoryMapper.TYPE_LIGHTING
            "BLUEBIN" -> WasteCategoryMapper.TYPE_PLASTIC
            else -> WasteCategoryMapper.TYPE_OTHERS
        }
    }

    private fun disableRecordVideo() {
        binding.btnRecordVideo.isEnabled = false
        binding.btnRecordVideo.isClickable = false

        // Visual clarity
        binding.btnRecordVideo.alpha = 1f
        binding.btnRecordVideo.text = "VIDEO SUBMITTED"

        // Force readable colors
        binding.btnRecordVideo.setBackgroundColor(
            ContextCompat.getColor(requireContext(), R.color.light_red_disabled)
        )
        binding.btnRecordVideo.setTextColor(Color.WHITE)
    }

    private fun enableRecordVideo() {
        binding.btnRecordVideo.isEnabled = true
        binding.btnRecordVideo.isClickable = true
        binding.btnRecordVideo.alpha = 1f
        binding.btnRecordVideo.text = "RECORD VIDEO"
        binding.btnRecordVideo.setBackgroundColor(
            ContextCompat.getColor(requireContext(), android.R.color.holo_blue_dark)
        )
        binding.btnRecordVideo.setTextColor(Color.WHITE)
    }

    // Validation orchestration function
    private fun handleSubmitWithValidation() {
        val quantity = currentCount
        val file = recordedFile

        // Quantity > 10 â†?video mandatory
        if (quantity > 10) {
            if (file == null) {
                showStatus("Video required for quantity > 10", isError = true)
                enableRecordVideo()
                return
            }
            uploadVideoAndSubmit(file)
            return
        }

        // Quantity <= 10 â†?check duration
        val durationSeconds = file?.let { getVideoDurationSeconds(it) } ?: 0

        if (durationSeconds > 5) {
            submitWithoutVideo(durationSeconds)
        } else {
            showStatus(
                "Invalid check-in: duration must be more than 5 seconds",
                isError = true
            )
            enableRecordVideo()
        }
    }

    // Upload video and submit (for quantity > 10)
    private fun uploadVideoAndSubmit(file: File) {
        val durationSeconds = getVideoDurationSeconds(file)

        updateSubmitButtonState(false)
        showStatus("Requesting upload permission...", isError = false)

        lifecycleScope.launch {
            try {
                // Step 1: Request pre-signed upload URL from backend
                val presignResponse = RetrofitClient.apiService().getPresignedUpload(
                    PresignUploadRequest(userId = userId.toLong())
                )

                if (!presignResponse.isSuccessful || presignResponse.body() == null) {
                    showStatus("Failed to get upload permission", isError = true)
                    updateSubmitButtonState(true)
                    enableRecordVideo()
                    return@launch
                }

                val presignData = presignResponse.body()!!
                Log.d(TAG, "Got pre-signed URL for key: ${presignData.objectKey}")

                // Step 2: Upload video directly to Spaces
                showStatus("Uploading video...", isError = false)

                val uploadSuccess = VideoUploader.uploadVideoToSpaces(
                    file = file,
                    presignedUrl = presignData.uploadUrl,
                    onProgress = { progress ->
                        lifecycleScope.launch {
                            showStatus("Uploading: $progress%", isError = false)
                        }
                    }
                )

                if (!uploadSuccess) {
                    showStatus("Video upload failed", isError = true)
                    updateSubmitButtonState(true)
                    enableRecordVideo()
                    return@launch
                }

                Log.d(TAG, "Video uploaded successfully")

                // Step 3: Submit check-in metadata WITH video key
                submitCheckIn(
                    durationSeconds = durationSeconds,
                    videoKey = presignData.objectKey
                )

            } catch (e: Exception) {
                Log.e(TAG, "Upload error", e)
                showStatus("Error: ${e.message}", isError = true)
                updateSubmitButtonState(true)
                enableRecordVideo()
            }
        }
    }

    // Submit WITHOUT video (valid low-quantity case)
    private fun submitWithoutVideo(durationSeconds: Int) {
        updateSubmitButtonState(false)
        showStatus("Submitting check-in...", isError = false)

        lifecycleScope.launch {
            try {
                submitCheckIn(
                    durationSeconds = durationSeconds,
                    videoKey = null
                )
            } catch (e: Exception) {
                Log.e(TAG, "Submission error", e)
                showStatus("Submission failed: ${e.message}", isError = true)
                updateSubmitButtonState(true)
            }
        }
    }

    // Centralized submission (clean & reusable)
    private suspend fun submitCheckIn(
        durationSeconds: Int,
        videoKey: String?
    ) {
        val checkInData = CheckInData(
            userId = userId,
            duration = durationSeconds.toLong(),
            binId = binId,
            wasteCategory = wasteType,
            quantity = currentCount,
            videoKey = videoKey
        )

        Log.d(ContentValues.TAG, "####Bin ID submitted: ${binId}")


        val response = RetrofitClient.apiService().submitRecycleCheckIn(checkInData)

        if (response.isSuccessful) {
            updateSubmitButtonState(false)
            disableRecordVideo()
            val resBody = response.body()
            if (resBody != null) {
                val message = resBody.responseCode + "\n" + resBody.responseDesc
                showStatus(message, isError = false)

                if (resBody.responseCode == "0000") {
                    isSubmitted = true
                    delay(1200)
                    findNavController().popBackStack()
                } else {
                    updateSubmitButtonState(true)
                    enableRecordVideo()
                }
            } else {
                showStatus("Empty server response", isError = true)
                updateSubmitButtonState(true)
                enableRecordVideo()
            }
        } else {
            showStatus("Submission failed (${response.code()})", isError = true)
            updateSubmitButtonState(true)
            enableRecordVideo()
        }
    }

    private fun showStatus(message: String, isError: Boolean = false) {
        binding.tvStatusMessage.apply {
            text = message
            setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (isError) android.R.color.holo_red_dark
                    else android.R.color.holo_green_dark
                )
            )
            visibility = View.VISIBLE
        }
    }

    private fun getVideoDurationSeconds(videoFile: File): Int {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(videoFile.absolutePath)

            val durationMs =
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 0L

            (durationMs / 1000).toInt()
        } finally {
            retriever.release()
        }
    }

    fun updateCounterDisplay() {
        binding.tvItemCount.text = currentCount.toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}



