package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
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
    private val radius = 100000.0

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
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
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
            displayBinInformation()
            preselectWasteTypeChip()
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    private fun preselectWasteTypeChip() {
        Log.d(TAG, "### preselectWasteTypeChip called with wasteCategory: $wasteCategory")

        if (wasteCategory.isNullOrEmpty()) {
            Log.d(TAG, "### wasteCategory is null or empty, skipping preselection")
            return
        }

        val normalizedCategory = wasteCategory?.trim()?.lowercase() ?: ""
        Log.d(TAG, "### Normalized category: $normalizedCategory")

        // Match based on contains instead of exact match
        val chipId = when {
            normalizedCategory.contains("plastic") -> R.id.chipPlastic
            normalizedCategory.contains("paper") -> R.id.chipPaper
            normalizedCategory.contains("glass") -> R.id.chipGlass
            normalizedCategory.contains("metal") -> R.id.chipMetal
            normalizedCategory.contains("e-waste") ||
                    normalizedCategory.contains("ewaste") ||
                    normalizedCategory.contains("electronic") -> R.id.chipEWaste
            normalizedCategory.contains("lighting") ||
                    normalizedCategory.contains("lamp") -> R.id.chipLighting
            else -> {
                Log.d(TAG, "### No matching chip for category: $normalizedCategory")
                null
            }
        }

        chipId?.let {
            binding.cgItemType.check(it)
            // Extract the base category for wasteType
            wasteType = when (it) {
                R.id.chipPlastic -> "Plastic"
                R.id.chipPaper -> "Paper"
                R.id.chipGlass -> "Glass"
                R.id.chipMetal -> "Metal"
                R.id.chipEWaste -> "E-Waste"
                R.id.chipLighting -> "Lighting"
                else -> ""
            }
            Log.d(TAG, "### Pre-selected chip ID: $it for category: $wasteCategory")
            Log.d(TAG, "### wasteType set to: $wasteType")
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
            wasteCategory = bundle.getString("wasteCategory")
        }
    }

    private fun displayBinInformation() {
        binding.apply {
            tvLocationName.text = binName.ifEmpty { "Recycling Bin" }
            tvLocationAddress.text = binAddress.ifEmpty { "Location not available" }
        }
    }

    private fun setupButtons() {
        binding.btnRecordVideo.setOnClickListener {
            requestPermissionNeeded()
        }

        binding.btnSubmit.setOnClickListener {
            handleSubmitWithValidation()
        }

        binding.btnBackToHome.setOnClickListener {
            findNavController().popBackStack()
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
            binding.cgItemType.setOnCheckedStateChangeListener { group, checkedIds ->
                if (checkedIds.isEmpty()) {
                    Log.d(TAG, "### No chip selected")
                    return@setOnCheckedStateChangeListener
                }

                val checkedId = checkedIds.first()
                wasteType = when (checkedId) {
                    R.id.chipPlastic -> "Plastic"
                    R.id.chipPaper -> "Paper"
                    R.id.chipGlass -> "Glass"
                    R.id.chipMetal -> "Metal"
                    R.id.chipEWaste -> "E-Waste"
                    R.id.chipLighting -> "Lighting"
                    else -> ""
                }
            }
        } catch (e: Exception) {
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
        } else if (findNavController().currentBackStackEntry?.savedStateHandle?.contains("item_count") == true) {
            loadLastRecordedVideo()
        }
    }

    private fun disableRecordVideo() {
        binding.btnRecordVideo.apply {
            isEnabled = false
            text = "VIDEO SUBMITTED"
            setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.light_red_disabled))
        }
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

    private fun handleSubmitWithValidation() {
        val quantity = currentCount
        val file = recordedFile

        if (quantity > 10) {
            if (file == null) {
                showStatus("Video required for quantity > 10", isError = true)
                enableRecordVideo()
                return
            }
            uploadVideoAndSubmit(file)
            return
        }

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

    private fun uploadVideoAndSubmit(file: File) {
        val durationSeconds = getVideoDurationSeconds(file)

        updateSubmitButtonState(false)
        disableRecordVideo()
        showStatus("Requesting upload permission...", isError = false)

        lifecycleScope.launch {
            try {
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

    private fun submitWithoutVideo(durationSeconds: Int) {
        updateSubmitButtonState(false)
        disableRecordVideo()
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
                enableRecordVideo()
            }
        }
    }

    private suspend fun submitCheckIn(
        durationSeconds: Int,
        videoKey: String?
    ) {
        val currentTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date())

        val checkInData = CheckInData(
            duration = durationSeconds.toLong(),
            binId = binId,
            wasteCategory = wasteType,
            quantity = currentCount,
            videoKey = videoKey,
            checkInTime = currentTime
        )

        Log.d(TAG, "####Bin ID submitted: ${binId}")
        Log.d(TAG, "####Submitting check-in with wasteType: $wasteType")

        val response = RetrofitClient.apiService().submitRecycleCheckIn(checkInData)

        if (response.isSuccessful) {
            val resBody = response.body()
            if (resBody != null) {
                if (resBody.responseCode == "SUCCESS") {
                    Log.d(TAG, "### Check-in successful, showing popup")
                    isSubmitted = true
                    showSuccessPopup()
                } else {
                    val message = resBody.responseCode + "\n" + resBody.responseDesc
                    showStatus(message, isError = true)
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

    private fun showSuccessPopup() {
        Log.d(TAG, "### showSuccessPopup called")

        try {
            val dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_success_checkin, null)

            val dialog = AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(false)
                .create()

            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            Log.d(TAG, "### Showing dialog")
            dialog.show()

            lifecycleScope.launch {
                delay(2500)
                Log.d(TAG, "### Dismissing dialog and navigating")

                if (dialog.isShowing) {
                    dialog.dismiss()
                }

                try {
                    findNavController().popBackStack(R.id.nav_home, false)
                    Log.d(TAG, "### Navigation successful")
                } catch (e: Exception) {
                    Log.e(TAG, "### Navigation failed: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "### Error showing popup: ${e.message}", e)
            // Fallback: just navigate
            findNavController().popBackStack(R.id.nav_home, false)
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