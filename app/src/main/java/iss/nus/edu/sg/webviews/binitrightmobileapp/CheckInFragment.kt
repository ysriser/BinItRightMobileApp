package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
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
    private var mappedWasteCategory: String? = null
    private var currentCount = 1
    private var _binding: FragmentCheckInBinding? = null
    private val binding get() = _binding!!
    private var isSubmitted = false
    private var recordedFile: File? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val supportedWasteTypes = setOf(
        "Plastic",
        "Paper",
        "Glass",
        "Metal",
        "E-Waste",
        "Lighting"
    )

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
                    getString(R.string.checkin_permission_required),
                    Toast.LENGTH_SHORT
                ).show()
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
            observeRecordedVideoState()
            displayBinInformation()
            preselectWasteTypeChip()
            updateCounterDisplay()
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    private fun preselectWasteTypeChip() {
        val fromCategory = ScannedCategoryHelper.toCheckInWasteType(wasteCategory)
        val fromBinType = ScannedCategoryHelper.toCheckInWasteTypeFromBinType(selectedBinType ?: binType)

        val preferredType = mappedWasteCategory
            ?.takeIf { it.isNotBlank() }
            ?: if (fromCategory != "Others") fromCategory else fromBinType

        Log.d(
            TAG,
            "preselectWasteTypeChip mapped=$preferredType raw=$wasteCategory selectedBinType=$selectedBinType"
        )

        val chipId = when (preferredType.lowercase()) {
            "plastic" -> R.id.chipPlastic
            "paper" -> R.id.chipPaper
            "glass" -> R.id.chipGlass
            "metal" -> R.id.chipMetal
            "e-waste" -> R.id.chipEWaste
            "lighting" -> R.id.chipLighting
            "others" -> R.id.chipOthers
            else -> null
        }

        chipId?.let {
            binding.cgItemType.check(it)
            wasteType = when (it) {
                R.id.chipPlastic -> "Plastic"
                R.id.chipPaper -> "Paper"
                R.id.chipGlass -> "Glass"
                R.id.chipMetal -> "Metal"
                R.id.chipEWaste -> "E-Waste"
                R.id.chipLighting -> "Lighting"
                R.id.chipOthers -> "Others"
                else -> ""
            }
            Log.d(TAG, "Pre-selected chip ID: $it, wasteType: $wasteType")
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
            selectedBinType = ScannedCategoryHelper.normalizeBinType(bundle.getString("selectedBinType"))
            wasteCategory = bundle.getString("wasteCategory")
            mappedWasteCategory = bundle.getString("mappedWasteCategory")

            if (mappedWasteCategory.isNullOrBlank()) {
                val fromCategory = ScannedCategoryHelper.toCheckInWasteType(wasteCategory)
                mappedWasteCategory = if (fromCategory != "Others") {
                    fromCategory
                } else {
                    ScannedCategoryHelper.toCheckInWasteTypeFromBinType(selectedBinType ?: binType)
                }
            }
        }
    }
    private fun displayBinInformation() {
        binding.apply {
            tvLocationName.text = binName.ifEmpty { getString(R.string.checkin_default_bin_name) }
            tvLocationAddress.text = binAddress.ifEmpty { getString(R.string.checkin_default_bin_address) }
        }
    }

    private fun setupButtons() {
        binding.btnRecordVideo.setOnClickListener {
            if (recordedFile != null) {
                recordedFile = null
                setVideoRecordedStatusVisible(false)
                updateSubmitButtonState(false)
                enableRecordVideo()
            }
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
        binding.btnDecrease.setOnClickListener {
            if (currentCount > 1) {
                currentCount--
                updateCounterDisplay()
            }
        }
        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<Int>("item_count")
            ?.observe(viewLifecycleOwner) {
                currentCount = maxOf(1, it)
                updateCounterDisplay()
            }
    }

    private fun updatePendingReviewHint() {
        setPendingReviewHintVisible(currentCount > 10)
    }

    private fun observeRecordedVideoState() {
        val handle = findNavController().currentBackStackEntry?.savedStateHandle ?: return
        handle.getLiveData<String>("recorded_video_path").observe(viewLifecycleOwner) { path ->
            if (path.isNullOrBlank()) return@observe
            val file = File(path)
            if (file.exists()) {
                recordedFile = file
                showVideoRecordedState()
                updateSubmitButtonState(true)
            } else {
                loadLastRecordedVideo()
            }
            handle.remove<String>("recorded_video_path")
            handle.remove<Boolean>("video_recorded")
        }

        handle.getLiveData<Boolean>("video_recorded").observe(viewLifecycleOwner) { recorded ->
            if (recorded == true && recordedFile == null) {
                loadLastRecordedVideo()
                handle.remove<Boolean>("video_recorded")
            }
        }
    }

    private fun setupChipListeners() {
        try {
            binding.cgItemType.setOnCheckedStateChangeListener { _, checkedIds ->
                if (checkedIds.isEmpty()) {
                    Log.d(TAG, "No chip selected")
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
                    R.id.chipOthers -> "Others"
                    else -> ""
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to bind chip listeners", e)
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
            Toast.makeText(requireContext(), getString(R.string.checkin_permission_required), Toast.LENGTH_SHORT).show()
            return
        }

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener { location ->
            if (location != null && LocationChecker.isWithinRadius(location.latitude, location.longitude, binLatitude, binLongitude, radius)) {
                val currentValue = maxOf(1, binding.tvItemCount.text.toString().toIntOrNull() ?: currentCount)
                findNavController().currentBackStackEntry?.savedStateHandle?.set("item_count", currentValue)
                findNavController().navigate(R.id.action_checkIn_to_videoRecord)
            } else {
                Toast.makeText(requireContext(), getString(R.string.checkin_near_bin_required), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadLastRecordedVideo() {
        if (isSubmitted) return
        val videoDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        val lastVideo = videoDir?.listFiles { it.extension == "mp4" }?.maxByOrNull { it.lastModified() }
        if (lastVideo != null) {
            recordedFile = lastVideo
            showVideoRecordedState()
            updateSubmitButtonState(true)
        }
    }

    override fun onResume() {
        super.onResume()
        if (isSubmitted) {
            disableRecordVideo()
            updateSubmitButtonState(false)
        } else {
            if (recordedFile != null) {
                showVideoRecordedState()
                updateSubmitButtonState(true)
            } else if (findNavController().currentBackStackEntry?.savedStateHandle?.contains("item_count") == true) {
                loadLastRecordedVideo()
            }
        }
    }

    private fun disableRecordVideo() {
        setVideoRecordedStatusVisible(true)
        binding.btnRecordVideo.apply {
            isEnabled = false
            text = getString(R.string.checkin_video_submitted)
            setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.light_red_disabled))
        }
    }

    private fun showVideoRecordedState() {
        setVideoRecordedStatusVisible(true)
        binding.btnRecordVideo.isEnabled = true
        binding.btnRecordVideo.text = getString(R.string.checkin_record_again)
        binding.btnRecordVideo.setBackgroundColor(
            ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark)
        )
        binding.btnRecordVideo.setTextColor(Color.WHITE)
    }

    private fun enableRecordVideo() {
        setVideoRecordedStatusVisible(false)
        binding.btnRecordVideo.isEnabled = true
        binding.btnRecordVideo.isClickable = true
        binding.btnRecordVideo.alpha = 1f
        binding.btnRecordVideo.text = getString(R.string.checkin_record_video)
        binding.btnRecordVideo.setBackgroundColor(
            ContextCompat.getColor(requireContext(), android.R.color.holo_blue_dark)
        )
        binding.btnRecordVideo.setTextColor(Color.WHITE)
    }

    private fun setPendingReviewHintVisible(visible: Boolean) {
        if (visible) {
            binding.tvPendingReviewHint.text = getString(R.string.checkin_pending_review_hint)
        } else {
            binding.tvPendingReviewHint.text = ""
        }
        setCollapsed(binding.tvPendingReviewHint, !visible)
    }

    private fun setVideoRecordedStatusVisible(visible: Boolean) {
        if (visible) {
            binding.tvVideoRecordedStatus.text = getString(R.string.checkin_video_recorded)
        } else {
            binding.tvVideoRecordedStatus.text = ""
        }
        setCollapsed(binding.tvVideoRecordedStatus, !visible)
    }

    private fun setCollapsed(view: View, collapsed: Boolean) {
        val params = view.layoutParams ?: return
        params.height = if (collapsed) 0 else ViewGroup.LayoutParams.WRAP_CONTENT
        view.layoutParams = params
        view.alpha = if (collapsed) 0f else 1f
        view.isEnabled = !collapsed
        view.isClickable = !collapsed
    }

    private fun handleSubmitWithValidation() {
        showStatus("")
        if (binId.isBlank()) {
            showStatus(getString(R.string.checkin_invalid_bin), isError = true)
            return
        }
        val resolvedWasteType = resolveWasteTypeForSubmission()
        if (resolvedWasteType == null) {
            showStatus(getString(R.string.checkin_invalid_item_type), isError = true)
            return
        }
        wasteType = resolvedWasteType

        val quantity = currentCount
        val file = recordedFile

        if (quantity > 10) {
            if (file == null) {
                showStatus(getString(R.string.checkin_video_required_quantity), isError = true)
                enableRecordVideo()
                return
            }
            uploadVideoAndSubmit(file)
            return
        }

        val durationSeconds = file?.let { getVideoDurationSeconds(it) } ?: 0

        if (durationSeconds >= 5) {
            submitWithoutVideo(durationSeconds)
        } else {
            showStatus(getString(R.string.checkin_video_duration_invalid), isError = true)
            enableRecordVideo()
        }
    }

    private fun resolveWasteTypeForSubmission(): String? {
        val candidates = listOf(
            wasteType,
            mappedWasteCategory,
            ScannedCategoryHelper.toCheckInWasteType(wasteCategory),
            ScannedCategoryHelper.toCheckInWasteTypeFromBinType(selectedBinType ?: binType)
        )

        return candidates
            .map { it.orEmpty().trim() }
            .firstOrNull { supportedWasteTypes.contains(it) }
    }

    private fun uploadVideoAndSubmit(file: File) {
        val durationSeconds = getVideoDurationSeconds(file)

        updateSubmitButtonState(false)
        disableRecordVideo()
        showStatus("")

        lifecycleScope.launch {
            try {
                val presignResponse = RetrofitClient.apiService().getPresignedUpload(
                    PresignUploadRequest(userId = userId.toLong())
                )

                if (!presignResponse.isSuccessful || presignResponse.body() == null) {
                    showStatus(getString(R.string.checkin_status_upload_permission_failed), isError = true)
                    updateSubmitButtonState(true)
                    enableRecordVideo()
                    return@launch
                }

                val presignData = presignResponse.body()!!
                Log.d(TAG, "Got pre-signed URL for key: ${presignData.objectKey}")

                val uploadSuccess = VideoUploader.uploadVideoToSpaces(
                    file = file,
                    presignedUrl = presignData.uploadUrl,
                    onProgress = { _ -> }
                )

                if (!uploadSuccess) {
                    showStatus(getString(R.string.checkin_status_upload_failed), isError = true)
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
        showStatus("")

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
                    showStatus("")
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
            val errorDetail = runCatching { response.errorBody()?.string() }
                .getOrNull()
                ?.replace("\n", " ")
                ?.trim()
                ?.take(160)
                .orEmpty()

            Log.e(
                TAG,
                "Check-in submission failed code=${response.code()} detail=$errorDetail"
            )
            if (errorDetail.isBlank()) {
                showStatus(
                    getString(R.string.checkin_status_submit_failed, response.code()),
                    isError = true
                )
            } else {
                showStatus(
                    getString(R.string.checkin_status_submit_failed_detail, response.code(), errorDetail),
                    isError = true
                )
            }
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

            dialog.window?.setBackgroundDrawable(
                ContextCompat.getDrawable(requireContext(), android.R.color.transparent)
            )

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
        if (message.isBlank()) {
            binding.tvStatusMessage.visibility = View.GONE
            return
        }
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
        binding.tvItemCount.text = getString(R.string.number_plain_int, currentCount)
        updatePendingReviewHint()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
