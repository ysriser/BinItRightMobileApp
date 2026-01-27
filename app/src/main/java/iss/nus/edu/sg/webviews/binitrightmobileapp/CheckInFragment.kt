package iss.nus.edu.sg.webviews.binitrightmobileapp
import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.os.Environment
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import kotlin.Int

class CheckInFragment : Fragment() {

    private val userId = 1;
    private val bin_lat = 1.290
    private val bin_lng = 103.77
    private val radius = 1000.0 // meters

    private val binId = 9
    private var currentCount = 0
    private val pointsPerItem = 10

    private var _binding: FragmentCheckInBinding? = null
    private val binding get() = _binding!!

    private var recordedFile: File? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient


    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()){
            isGranted -> if(isGranted){
                checkLocationAndNavigate()
        } else {
            Toast.makeText(
                requireContext(),
                "Location permission is required to check in.",
                Toast.LENGTH_SHORT).show()
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

        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(requireActivity())

        binding.btnRecordVideo.setOnClickListener {
            requestPermissionNeeded()
        }

        binding.btnSubmit.setOnClickListener {
            uploadVideo()
        }

        binding.btnIncrease.setOnClickListener {
            currentCount++
            updateCounterDisplay()
        }

        binding.btnDecrease.setOnClickListener {
            if (currentCount > 1) { // Prevent going below 1
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

    fun requestPermissionNeeded(){
        when{
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION)==
                    PackageManager.PERMISSION_GRANTED->{
                        // Permission already granted
                        checkLocationAndNavigate()
                    }

            // Returns true if the user previously denied the permission
            shouldShowRequestPermissionRationale(
                Manifest.permission.ACCESS_FINE_LOCATION)->{
                    Toast.makeText(requireContext(),
                        "Location access is needed to verify your recycling location.",
                        Toast.LENGTH_SHORT).show()

                //Ask the user for fine location permission
                locationPermissionLauncher.launch(
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
                }
            else->{
                //First-time request
                locationPermissionLauncher.launch(
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            }
        }
    }

    fun checkLocationAndNavigate(){

        //Permission Check
        if(ContextCompat.checkSelfPermission(
            requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION)!=
            PackageManager.PERMISSION_GRANTED){
            Toast.makeText(
                requireContext(),
                "Location permission is required to check in.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        //Get last known location
        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            null
        ).addOnSuccessListener{
            location -> if(location == null){
            Toast.makeText(
                requireContext(),
                "Unable to detect your location. Please try again.",
                Toast.LENGTH_SHORT
            ).show()
            return@addOnSuccessListener  //just stop this lambda
        }

            // Radius validation if location found
            val isWithinRange = LocationChecker.isWithinRadius(
                location.latitude,
                location.longitude,
                bin_lat, bin_lng, radius
            )

            if(isWithinRange){
                // Save check in data before navigating
                findNavController()
                    .currentBackStackEntry
                    ?.savedStateHandle
                    ?.set("item_count", currentCount)

                // Allow navigation to VideoRecordFragment
                findNavController().navigate(R.id.action_checkIn_to_videoRecord)
            } else {
                // Block navigation
                Toast.makeText(
                    requireContext(),
                    "You must be near recycling bin to record a video.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun loadLastRecordedVideo() {
        val videoDir =
            requireContext().getExternalFilesDir(Environment.DIRECTORY_MOVIES)
                ?: run {
                    binding.btnSubmit.isEnabled = false
                    return
                }
        val lastVideo = videoDir
            .listFiles { file -> file.extension == "mp4" }
            ?.maxByOrNull { it.lastModified() }

        if (lastVideo != null) {
            recordedFile = lastVideo
            binding.btnSubmit.isEnabled = true
        } else {
            binding.btnSubmit.isEnabled = false
        }
    }

    override fun onResume() {
        super.onResume()
        loadLastRecordedVideo()
    }
    private fun uploadVideo(){
        val file = recordedFile?:run{
            Toast.makeText(
                requireContext(),
                "No Video to upload",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val durationSeconds = getVideoDurationSeconds(file)

        val checkInData = CheckInData(
            userId = userId,
            recordedAt = System.currentTimeMillis(),
            duration=durationSeconds,
            binId=binId,
            itemId = 1,
            quantity = currentCount
        )


        val gson = Gson()  // library to convert Kotlin objects to JSON string
        val metadataJson = gson.toJson(checkInData)

        // Convert Json String to OkHttp RequestBody
        val metadataBody = metadataJson.
        toRequestBody("application/json".toMediaType())

        // Convert videoFile to OkHttp RequestBody for Http upload
        val videoBody = file.asRequestBody("video/mp4".toMediaType())

        val videoPart = MultipartBody.Part.createFormData(
            name = "video",
            filename = file.name,
            body = videoBody
        )

        // Upload
        lifecycleScope.launch{
            try{
                binding.btnSubmit.isEnabled = true

                val response = RetrofitClient.api.submitRecycleCheckIn(
                    video = videoPart,
                    metadata = metadataBody
                )

                if(response.isSuccessful){
                    val resBody = response.body()
                    if (resBody != null) {
                        val message = resBody.responseCode +"\n"+resBody.responseDesc
                        showStatus(message, isError = false)

                        if (resBody.responseCode == "0000") {
                            delay(1200) // allow user to read success message
                            findNavController().popBackStack()
                        }
                        } else {
                        showStatus("Empty server response", isError = true)
                    }
                }
                else {
                    showStatus(
                        "Submission failed (${response.code()})",
                        isError = true
                    )
                    binding.btnSubmit.isEnabled = true
                }

            } catch (e: Exception){
                showStatus(
                    "Network error. Please try again.",
                    isError = true
                )
                binding.btnSubmit.isEnabled = true
            }
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

            (durationMs / 1000).toInt()   // convert ms → seconds
        } finally {
            retriever.release()
        }
    }

    fun updateCounterDisplay() {
        binding.tvItemCount.text = currentCount.toString()
        val totalPoints = currentCount * pointsPerItem
        binding.tvPointsInfo.text = "You'll earn $totalPoints points"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
