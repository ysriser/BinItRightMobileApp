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

    private var currentScanResult: ScanResult? = null

    private fun displayResult(scanResult: ScanResult) {
        currentScanResult = scanResult
        binding.tvCategory.text = mappingCategory(scanResult.category)

        val isNotSure = isNotSureCategory(scanResult.category)

        if (isNotSure) {
            binding.tvBadge.text = "Not sure"
            binding.tvBadge.setTextColor(
                androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.holo_orange_dark)
            )
            binding.tvBadge.setBackgroundResource(R.drawable.bg_badge_recyclable)

            binding.ivSuccess.setImageResource(R.drawable.ic_help_24)
            binding.ivSuccess.setColorFilter(
                androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.holo_orange_dark)
            )

            binding.tvDescriptionWait.text = "We are not fully sure about this item."
            binding.tvDescriptionWait.setTextColor(
                androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.holo_orange_dark)
            )
        } else if (scanResult.recyclable) {
            binding.tvBadge.text = "Recyclable"
            binding.tvBadge.setTextColor(
                androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark)
            )
            binding.tvBadge.setBackgroundResource(R.drawable.bg_badge_recyclable)

            binding.ivSuccess.setImageResource(R.drawable.ic_check_circle_24)
            binding.ivSuccess.setColorFilter(
                androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark)
            )

            binding.tvDescriptionWait.text = "Great news! This item can be recycled."
            binding.tvDescriptionWait.setTextColor(
                androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark)
            )
        } else {
            binding.tvBadge.text = "Not Recyclable"
            binding.tvBadge.setTextColor(
                androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)
            )
            binding.tvBadge.setBackgroundResource(R.drawable.bg_badge_recyclable)

            binding.ivSuccess.setImageResource(R.drawable.ic_close_24)
            binding.ivSuccess.setColorFilter(
                androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)
            )

            binding.tvDescriptionWait.text = "This item cannot be recycled."
            binding.tvDescriptionWait.setTextColor(
                androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)
            )
        }
        binding.tvBadge.isVisible = true

        if (!scanResult.instruction.isNullOrBlank()) {
            binding.tvDescriptionWait.text = scanResult.instruction
        } else if (scanResult.instructions.isNotEmpty()) {
            binding.tvDescriptionWait.text = scanResult.instructions.first()
        }

        val steps = if (scanResult.instructions.isNotEmpty()) {
            scanResult.instructions
        } else if (!scanResult.instruction.isNullOrBlank()) {
            listOf(scanResult.instruction)
        } else {
            emptyList()
        }

        binding.tvInstructionTitle.text = "How to dispose:"
        binding.tvInstructionSteps.text = if (steps.isNotEmpty()) {
            steps.mapIndexed { index, tip -> "${index + 1}. $tip" }.joinToString("\n")
        } else {
            "1. Follow local disposal guidance"
        }
    }

    private fun mappingCategory(fullCategory: String): String {
        val trimmed = fullCategory.trim()
        return when {
            trimmed.startsWith("E-waste - ", ignoreCase = true) -> "E-waste"
            trimmed.startsWith("Textile - ", ignoreCase = true) -> "Textile"
            else -> trimmed
        }
    }

    private fun isNotSureCategory(category: String): Boolean {
        val normalized = category.trim().lowercase()
        return normalized.contains("not sure")
                || normalized.contains("uncertain")
                || normalized.contains("unknown")
                || normalized.contains("other_uncertain")
    }

    private fun setupListeners() {
        binding.btnScanAgain.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnNotNow.setOnClickListener {
            // Navigate home or exit
            // Navigate to Home
            findNavController().popBackStack(R.id.nav_home, false)

        }

        binding.btnRecycle.setOnClickListener {
            // Get the scanned item type
            val scannedCategory = mappingCategory(currentScanResult?.category ?: "")
            // Use the bin type determined by the Repository/Logic
            val binType = currentScanResult?.binType ?: ""

            val bundle = Bundle().apply {
                putString("selectedBinType", binType)
                putString("wasteCategory", scannedCategory)
            }

            findNavController().navigate(
                R.id.action_scanningResultFragment_to_nearbyBinFragment,
                bundle
            )
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
