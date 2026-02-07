package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.Manifest
import android.annotation.SuppressLint
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
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.CheckInData
import iss.nus.edu.sg.webviews.binitrightmobileapp.network.RetrofitClient
import iss.nus.edu.sg.webviews.binitrightmobileapp.utils.JwtUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CheckInFragment : Fragment() {

    private var userId: Long = -1L
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
                Toast.makeText(requireContext(), "Permission required.", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCheckInBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {

            val prefs = requireContext().getSharedPreferences("APP_PREFS", Context.MODE_PRIVATE)

            val token = prefs.getString("TOKEN", null) ?: prefs.getString("JWT_TOKEN", null)
            if (token != null) {
                userId = JwtUtils.getUserIdFromToken(token) ?: -1L
                Log.d(TAG, "Successfully parsed userId from Token: $userId")
            }

            if (userId == -1L) {
                userId = prefs.getLong("USER_ID", -1L)
            }

            recordedFile = null
            retrieveBinInformation()
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
            updateSubmitButtonState(false)

            setupButtons()
            setupCounter()
            setupChipListeners()

            retrieveScannedBinType()

            displayBinInformation()

            Log.d(TAG, "### Resolved User ID: $userId")

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun retrieveBinInformation() {
        arguments?.let { bundle ->
            binId = bundle.getString("binId") ?: ""
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
    }

    private fun displayBinInformation() {
        binding.apply {
            tvLocationName.text = binName.ifEmpty { "Recycling Bin" }
            tvLocationAddress.text = binAddress.ifEmpty { "Location not available" }
        }
    }

    private fun setupButtons() {
        binding.btnBackHome.setOnClickListener { findNavController().navigate(R.id.action_checkInFragment_to_homeFragment) }
        binding.btnBackToHome.setOnClickListener { findNavController().popBackStack() }
        binding.btnRecordVideo.setOnClickListener { requestPermissionNeeded() }
        binding.btnSubmit.setOnClickListener {
            disableRecordVideo()
            handleSubmitWithValidation()
        }
    }

    private fun setupCounter() {
        binding.btnIncrease.setOnClickListener { currentCount++; updateCounterDisplay() }
        binding.btnDecrease.setOnClickListener { if (currentCount > 0) { currentCount--; updateCounterDisplay() } }
        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<Int>("item_count")
            ?.observe(viewLifecycleOwner) { currentCount = it; updateCounterDisplay() }
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
            alpha = if (enabled) 1.0f else 0.5f
            setBackgroundColor(ContextCompat.getColor(requireContext(), if (enabled) android.R.color.holo_green_dark else android.R.color.darker_gray))
        }
    }

    private fun requestPermissionNeeded() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            checkLocationAndNavigate()
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    @SuppressLint("MissingPermission")
    fun checkLocationAndNavigate() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(requireContext(), "Permission required.", Toast.LENGTH_SHORT).show()
            return
        }

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener { location ->
            if (location != null && LocationChecker.isWithinRadius(location.latitude, location.longitude, binLatitude, binLongitude, radius)) {
                val currentValue = binding.tvItemCount.text.toString().toIntOrNull() ?: 0
                findNavController().currentBackStackEntry?.savedStateHandle?.set("item_count", currentValue)
                findNavController().navigate(R.id.action_checkIn_to_videoRecord)
            } else {
                Toast.makeText(requireContext(), "You must be near the recycling bin.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadLastRecordedVideo() {
        if (isSubmitted) return
        val videoDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        val lastVideo = videoDir?.listFiles { it.extension == "mp4" }?.maxByOrNull { it.lastModified() }
        if (lastVideo != null) {
            recordedFile = lastVideo
            updateSubmitButtonState(true)
        }
    }

    override fun onResume() {
        super.onResume()
        if (isSubmitted) {
            disableRecordVideo()
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

    private fun restoreRecordVideoButton() {
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

        // Quantity > 10 video mandatory
        if (quantity > 10) {
            if (file == null) {
                showStatus("Video required for quantity > 10", isError = true)
                restoreRecordVideoButton()
                return
            }
            submitWithVideoProof(file)
            return
        }

        // Quantity <= 10 check duration
        val durationSeconds = file?.let { getVideoDurationSeconds(it) } ?: 0

        if (durationSeconds > 5) {
            submitWithoutVideoProof(durationSeconds)
        } else {
            showStatus(
                "Invalid check-in: duration must be more than 5 seconds",
                isError = true
            )
            restoreRecordVideoButton()
        }
    }

    // Upload video and submit (for quantity > 10)
    private fun submitWithVideoProof(file: File) {
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
                    restoreRecordVideoButton()
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
                    restoreRecordVideoButton()
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
                restoreRecordVideoButton()
            }
        }
    }

    // Submit WITHOUT video (valid low-quantity case)
    private fun submitWithoutVideoProof(durationSeconds: Int) {
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
        val currentTime = SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss",
            Locale.getDefault()
        ).format(Date())

        val checkInData = CheckInData(
            userId = userId,
            duration = durationSeconds.toLong(),
            binId = binId,
            wasteCategory = wasteType,
            quantity = currentCount,
            videoKey = videoKey,
            checkInTime = currentTime
        )

        Log.d(ContentValues.TAG, "####Bin ID submitted: ${binId}")

        try {
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
                        restoreRecordVideoButton()
                    }
                } else {
                    showStatus("Error: ${response.body()?.responseDesc ?: response.message()}", true)
                    updateSubmitButtonState(true)
                    restoreRecordVideoButton()
                }
            } else {
                showStatus("Submission failed (${response.code()})", true)
                updateSubmitButtonState(true)
                restoreRecordVideoButton()
            }
        } catch (e: Exception) {
            showStatus("Network Error: ${e.message}", true)
            updateSubmitButtonState(true)
            restoreRecordVideoButton()
        }
    }
    private fun showStatus(message: String, isError: Boolean) {
        binding.tvStatusMessage.apply {
            text = message
            setTextColor(ContextCompat.getColor(requireContext(), if (isError) android.R.color.holo_red_dark else android.R.color.holo_green_dark))
            visibility = View.VISIBLE
        }
    }

    private fun getVideoDurationSeconds(videoFile: File): Int {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(videoFile.absolutePath)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
            (durationMs / 1000).toInt()
        } catch (e: Exception) { 0 } finally { retriever.release() }
    }

    fun updateCounterDisplay() { binding.tvItemCount.text = currentCount.toString() }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}




