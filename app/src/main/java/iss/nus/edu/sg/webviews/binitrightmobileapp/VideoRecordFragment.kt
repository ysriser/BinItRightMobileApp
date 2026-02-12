package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import iss.nus.edu.sg.webviews.binitrightmobileapp.databinding.FragmentVideoRecordBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

class VideoRecordFragment : Fragment() {
    private var _binding: FragmentVideoRecordBinding? = null
    private val binding get() = _binding!!

    private lateinit var videoCapture: VideoCapture<Recorder>
    private var recording: Recording? = null
    private var currentVideoFile: File? = null
    private var discardOnFinalize = false
    private var recordingStartAtMs: Long = 0L
    private var recordedDurationSeconds: Int = 0
    private var timerJob: Job? = null
    private var isCameraReady = false

    companion object {
        private const val MIN_RECORD_SECONDS = 5
    }

    private fun hasPermissions(): Boolean =
        ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val granted = permissions.all { it.value }
            if (granted) {
                startCamera()
            } else {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.video_permissions_required),
                    Toast.LENGTH_LONG
                ).show()
                findNavController().popBackStack()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVideoRecordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateIdleUi()

        if (hasPermissions()) {
            startCamera()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
                )
            )
        }

        binding.btnStartRecording.setOnClickListener {
            if (hasPermissions()) {
                @SuppressLint("MissingPermission")
                startRecording()
            } else {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO
                    )
                )
            }
        }
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun startRecording() {
        if (recording != null) {
            val elapsedSeconds = getEffectiveRecordedSeconds()
            if (recordingStartAtMs > 0L && elapsedSeconds < MIN_RECORD_SECONDS) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.video_recording_too_short),
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
            discardOnFinalize = false
            stopRecording()
            updateIdleUi()
            Toast.makeText(
                requireContext(),
                getString(R.string.video_recording_stopped),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (!::videoCapture.isInitialized || !isCameraReady) {
            Toast.makeText(
                requireContext(),
                getString(R.string.video_camera_initializing),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        currentVideoFile = File(
            requireContext().getExternalFilesDir(Environment.DIRECTORY_MOVIES),
            "recycle_${System.currentTimeMillis()}.mp4"
        )
        val outputOptions = FileOutputOptions.Builder(currentVideoFile!!).build()

        discardOnFinalize = false
        recordingStartAtMs = System.currentTimeMillis()
        recordedDurationSeconds = 0
        binding.tvRecordingTimer.text = getString(R.string.video_record_timer_default)
        startTimer()
        updateRecordingUi()

        recording = videoCapture.output
            .prepareRecording(requireContext(), outputOptions)
            .withAudioEnabled()
            .start(
                ContextCompat.getMainExecutor(requireContext())
            ) { event ->
                when (event) {
                    is VideoRecordEvent.Status -> {
                        recordedDurationSeconds =
                            (event.recordingStats.recordedDurationNanos / 1_000_000_000L).toInt()
                        binding.tvRecordingTimer.text = formatTime(recordedDurationSeconds)
                    }
                    is VideoRecordEvent.Finalize -> handleFinalize(event)
                    else -> Unit
                }
            }
    }

    private fun stopRecording() {
        recording?.stop()
        recording = null
    }

    private fun handleFinalize(event: VideoRecordEvent.Finalize) {
        resetRecordingState()
        updateIdleUi()

        val file = currentVideoFile
        currentVideoFile = null

        if (event.hasError()) {
            file?.delete()
            Toast.makeText(requireContext(), getString(R.string.video_recording_stopped), Toast.LENGTH_SHORT).show()
            return
        }

        if (discardOnFinalize) {
            file?.delete()
            findNavController().popBackStack()
            return
        }

        val savedFile = file
        if (savedFile != null && savedFile.exists()) {
            findNavController()
                .previousBackStackEntry
                ?.savedStateHandle
                ?.set("video_recorded", true)
            findNavController()
                .previousBackStackEntry
                ?.savedStateHandle
                ?.set("recorded_video_path", savedFile.absolutePath)
            Toast.makeText(
                requireContext(),
                getString(R.string.video_recording_saved),
                Toast.LENGTH_SHORT
            ).show()
        }
        findNavController().popBackStack()
    }

    private fun resetRecordingState() {
        timerJob?.cancel()
        timerJob = null
        recordingStartAtMs = 0L
        recordedDurationSeconds = 0
        binding.tvRecordingTimer.text = getString(R.string.video_record_timer_default)
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewLifecycleOwner.lifecycleScope.launch {
            while (isActive && recording != null) {
                binding.tvRecordingTimer.text = formatTime(getEffectiveRecordedSeconds())
                delay(250)
            }
        }
    }

    private fun getEffectiveRecordedSeconds(): Int {
        val clockBasedSeconds = if (recordingStartAtMs <= 0L) {
            0
        } else {
            ((System.currentTimeMillis() - recordingStartAtMs) / 1000L).toInt()
        }
        return maxOf(recordedDurationSeconds, clockBasedSeconds)
    }

    private fun formatTime(totalSeconds: Int): String {
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }

    private fun updateIdleUi() {
        binding.btnStartRecording.text = getString(R.string.video_action_start_recording)
        binding.btnStartRecording.setIconResource(R.drawable.ic_videocam_24)
        binding.btnStartRecording.backgroundTintList =
            ContextCompat.getColorStateList(requireContext(), R.color.brand_green)
        binding.tvMinDurationHint.text = getString(R.string.video_record_min_duration)
    }

    private fun updateRecordingUi() {
        binding.btnStartRecording.text = getString(R.string.video_action_finish_save)
        binding.btnStartRecording.setIconResource(R.drawable.ic_close_24)
        binding.btnStartRecording.backgroundTintList =
            ContextCompat.getColorStateList(requireContext(), R.color.color_warning)
        binding.tvMinDurationHint.text = getString(R.string.video_recording_in_progress)
    }

    private fun startCamera() {
        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                .build()

            videoCapture = VideoCapture.withOutput(recorder)
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                viewLifecycleOwner,
                cameraSelector,
                preview,
                videoCapture
            )
            isCameraReady = true
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        isCameraReady = false
        discardOnFinalize = true
        resetRecordingState()
        stopRecording()
        _binding = null
    }
}
