package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import coil.load
import iss.nus.edu.sg.webviews.binitrightmobileapp.databinding.FragmentScanningResultBinding
import java.io.File

class ScanningResultFragment : Fragment() {

    private var _binding: FragmentScanningResultBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ScanViewModel by viewModels {
        ScanViewModelFactory(RealScanRepository())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScanningResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Retrieve using Bundle instead of SafeArgs
        val imageUriString = arguments?.getString("imageUri")
        
        if (imageUriString != null) {
            val uri = Uri.parse(imageUriString)
            // Load image using Coil
            binding.ivCapturedImage.load(uri)

            // Auto-trigger scan if file exists
            if (uri.scheme == "file") {
                val file = File(uri.path!!)
                viewModel.scanImage(file)
            } else {
                Toast.makeText(context, "Invalid image URI", Toast.LENGTH_SHORT).show()
            }
        }

        setupObservers()
        setupListeners()
    }

    private fun setupObservers() {
        viewModel.scanResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess { scanResult ->
                binding.tvCategory.text = scanResult.category
                binding.tvBadge.text = if (scanResult.recyclable) "♻ Recyclable" else "Not Recyclable"
                binding.tvBadge.isVisible = true
                binding.tvDescriptionWait.text = if (scanResult.recyclable) 
                    "Great news! This item can be recycled. ✨" 
                else 
                    "This item cannot be recycled."
                // For instructions/benefits, we could parse from scanResult.instructions
            }.onFailure {
                Toast.makeText(context, "Scan failed: ${it.message}", Toast.LENGTH_SHORT).show()
                 // fallback UI or retry
            }
        }

        viewModel.feedbackStatus.observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                Toast.makeText(context, "Feedback sent!", Toast.LENGTH_SHORT).show()
                binding.btnAccurate.isEnabled = false
                binding.btnIncorrect.isEnabled = false
            }
        }
    }

    private fun setupListeners() {
        binding.btnScanAgain.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnNotNow.setOnClickListener {
            // Navigate home or exit
            findNavController().popBackStack(R.id.homeFragment, false)

        }

        binding.btnRecycle.setOnClickListener {
            Toast.makeText(context, "Proceed to check-in flow (TODO)", Toast.LENGTH_SHORT).show()
        }

        binding.btnAccurate.setOnClickListener {
           sendFeedback(true)
        }

        binding.btnIncorrect.setOnClickListener {
            sendFeedback(false)
        }
    }

    private fun sendFeedback(isAccurate: Boolean) {
        val imageId = "temp_image_id" // In real app, get from ScanResult
        val feedback = FeedbackRequest(
            imageId = imageId,
            userFeedback = isAccurate,
            timestamp = System.currentTimeMillis()
        )
        viewModel.submitFeedback(feedback)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
