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
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import iss.nus.edu.sg.webviews.binitrightmobileapp.databinding.FragmentVideoRecordBinding
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.core.content.ContextCompat
import java.io.File


class VideoRecordFragment : Fragment() {
    private var _binding: FragmentVideoRecordBinding? = null
    private val binding get() = _binding!!

    private lateinit var videoCapture: VideoCapture<Recorder>
    private var recording: Recording? = null

    // Permissions
    private fun hasPermissions(): Boolean =
        ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    requireContext(), Manifest.permission.RECORD_AUDIO
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
    ): View? {
        _binding = FragmentVideoRecordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

        binding.btnClose.setOnClickListener {
            findNavController().popBackStack()
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
            recording?.stop()
            recording = null

            binding.btnStartRecording.text = getString(R.string.video_action_start)

            Toast.makeText(requireContext(), getString(R.string.video_recording_stopped), Toast.LENGTH_SHORT).show()
            return
        }

        val videoFile = File(
            requireContext().getExternalFilesDir(Environment.DIRECTORY_MOVIES),
            "recycle_${System.currentTimeMillis()}.mp4"
        )

        val outputOptions = FileOutputOptions.Builder(videoFile).build()

        binding.btnStartRecording.text = getString(R.string.video_action_stop)

        recording = videoCapture.output
            .prepareRecording(requireContext(), outputOptions)
            .withAudioEnabled()
            .start(
                ContextCompat.getMainExecutor(requireContext())
            ) { event ->
                if (event is VideoRecordEvent.Finalize && !event.hasError()) {
                    // Notify CheckInFragment that video was recorded
                    findNavController()
                        .previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("video_recorded", true)

                    findNavController().popBackStack()
                }
            }

    }

    private fun startCamera(){
        // Gets CameraX provider - entrypoint to access phone camera
        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(requireContext())

        // CameraX initialization inside addListener
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get() // Camera ready

            // Create preview (screen display)
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            val recorder = Recorder.Builder().
            setQualitySelector(QualitySelector.from(Quality.HIGHEST)).build()

            // record button
            videoCapture = VideoCapture.withOutput(recorder)

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                viewLifecycleOwner, cameraSelector,
                preview, videoCapture)

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        recording?.stop()
        recording = null
        _binding = null
    }
}
