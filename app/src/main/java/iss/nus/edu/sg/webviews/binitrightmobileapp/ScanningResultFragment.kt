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
        ScanViewModelFactory(requireContext())
    }
    
    private var scanAnimator: android.animation.ObjectAnimator? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScanningResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Retrieve using Bundle
        val imageUriString = arguments?.getString("imageUri")
        // Retrieve result object
        val scanResult = arguments?.getSerializable("scanResult") as? ScanResult
        
        if (imageUriString != null) {
            val uri = Uri.parse(imageUriString)
            binding.ivCapturedImage.load(uri)

            if (scanResult != null) {
                // Display result directly
                displayResult(scanResult)
            } else {
                // Fallback: Auto-trigger scan if file exists and no result passed
                if (uri.scheme == "file") {
                    val file = File(uri.path!!)
                    viewModel.scanImage(file)
                }
            }
        }

        setupObservers()
        setupListeners()
    }

    private fun setupObservers() {
        viewModel.scanResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                it.onSuccess { scanResult ->
                   displayResult(scanResult)
                }.onFailure { error ->
                    Toast.makeText(context, "Scan failed: ${error.message}", Toast.LENGTH_SHORT).show()
                }
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
    
    private fun displayResult(scanResult: ScanResult) {
        binding.tvCategory.text = scanResult.category
        
        if (scanResult.recyclable) {
            binding.tvBadge.text = "♻ Recyclable"
            binding.tvBadge.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark))
            binding.tvBadge.setBackgroundResource(R.drawable.bg_badge_recyclable) // Ensure this exists or use tint
            
            binding.ivSuccess.setImageResource(R.drawable.ic_check_circle_24)
            binding.ivSuccess.setColorFilter(androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark))
            
            binding.tvDescriptionWait.text = "Great news! This item can be recycled. ✨"
            binding.tvDescriptionWait.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark))
        } else {
            binding.tvBadge.text = "Not Recyclable"
            // Use a darker gray or red for non-recyclable
            binding.tvBadge.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))
            // binding.tvBadge.setBackgroundResource(...) // Optional
            
            // Change icon to 'Cancel' or similar
            binding.ivSuccess.setImageResource(R.drawable.ic_close_24) // Reusing existing close icon if available, or just check-circle with red tint
            binding.ivSuccess.setColorFilter(androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))
            
            binding.tvDescriptionWait.text = "This item cannot be recycled."
            binding.tvDescriptionWait.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))
        }
        binding.tvBadge.isVisible = true
        
        // Overwrite description if instruction exists
        if (!scanResult.instruction.isNullOrBlank()) {
             binding.tvDescriptionWait.text = scanResult.instruction
        } else if (scanResult.instructions.isNotEmpty()) {
            binding.tvDescriptionWait.text = scanResult.instructions.mapIndexed { index, tip -> "${index + 1}. $tip" }.joinToString("\n")
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
