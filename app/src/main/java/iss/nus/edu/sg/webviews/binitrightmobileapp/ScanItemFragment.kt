package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import iss.nus.edu.sg.webviews.binitrightmobileapp.databinding.FragmentScanItemBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScanItemFragment : Fragment() {

    private var _binding: FragmentScanItemBinding? = null
    private val binding get() = _binding!!

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService

    private val viewModel: ScanViewModel by viewModels {
        ScanViewModelFactory(requireContext())
    }

    private var scanAnimator: android.animation.ObjectAnimator? = null
    private var lastCapturedUri: Uri? = null

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                startCamera()
            } else {
                Toast.makeText(requireContext(), "Camera permission required", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScanItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraExecutor = Executors.newSingleThreadExecutor()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        binding.btnTakePhoto.setOnClickListener {
            takePhoto()
        }

        binding.btnClose.setOnClickListener {
            findNavController().popBackStack(R.id.nav_home, false)
        }

        binding.switchDebug.setOnCheckedChangeListener { _, isChecked ->
            viewModel.toggleDebugMode(isChecked)
        }

        setupObservers()
    }

    private fun takePhoto() {
        val captureUseCase = imageCapture
        if (captureUseCase == null) {
            Toast.makeText(requireContext(), "Camera is not ready yet", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnTakePhoto.isEnabled = false

        val photoFile = File(
            getOutputDirectory(),
            SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + ".jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        captureUseCase.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                    binding.btnTakePhoto.isEnabled = true
                    Toast.makeText(requireContext(), "Failed to capture photo", Toast.LENGTH_SHORT).show()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    lastCapturedUri = savedUri
                    Log.d(TAG, "Photo capture succeeded: $savedUri")

                    startScanUI()
                    viewModel.scanImage(photoFile)
                }
            }
        )
    }

    private fun setupObservers() {
        viewModel.scanningStatus.observe(viewLifecycleOwner) { status ->
            binding.tvScanProgress.text = status
        }

        viewModel.scanResult.observe(viewLifecycleOwner) { result ->
            if (result == null) {
                return@observe
            }

            result.onSuccess { scanResult ->
                stopScanUI()

                val bundle = Bundle().apply {
                    putString("imageUri", lastCapturedUri?.toString())
                    putSerializable("scanResult", scanResult)
                }

                findNavController().navigate(R.id.action_scanItemFragment_to_scanningResultFragment, bundle)
                viewModel.resetScanState()
            }.onFailure {
                stopScanUI()
                Toast.makeText(context, "Scan failed: ${it.message}", Toast.LENGTH_LONG).show()
                binding.btnTakePhoto.isEnabled = true
                viewModel.resetScanState()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.btnTakePhoto.isEnabled = true
        binding.loadingOverlay.isVisible = false
        viewModel.resetScanState()
    }

    private fun startScanUI() {
        binding.loadingOverlay.isVisible = true
        binding.scanLine.translationX = 0f

        val width = binding.loadingOverlay.width.takeIf { it > 0 }?.toFloat() ?: 1000f

        scanAnimator = android.animation.ObjectAnimator.ofFloat(binding.scanLine, "translationX", 0f, width).apply {
            duration = 1500
            repeatCount = android.animation.ValueAnimator.INFINITE
            repeatMode = android.animation.ValueAnimator.RESTART
            start()
        }
    }

    private fun stopScanUI() {
        scanAnimator?.cancel()
        binding.loadingOverlay.isVisible = false
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            if (!isAdded || _binding == null) {
                return@addListener
            }

            val cameraProvider = try {
                cameraProviderFuture.get()
            } catch (exc: Exception) {
                Log.e(TAG, "Failed to get camera provider", exc)
                Toast.makeText(requireContext(), "Unable to start camera", Toast.LENGTH_SHORT).show()
                return@addListener
            }

            val preview = Preview.Builder()
                .build()
                .also { previewUseCase ->
                    previewUseCase.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    viewLifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
                Toast.makeText(requireContext(), "Camera initialization failed", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun allPermissionsGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun getOutputDirectory(): File {
        val mediaDir = requireContext().externalCacheDir?.let {
            File(it, "BinItImages").apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists()) mediaDir else requireContext().filesDir
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        cameraExecutor.shutdown()
    }

    companion object {
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val TAG = "ScanItemFragment"
    }
}
