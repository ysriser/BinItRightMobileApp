package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.net.toUri
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

    private var currentScanResult: ScanResult? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScanningResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val imageUriString = arguments?.getString("imageUri")
        val scanResult = arguments?.getSerializable("scanResult") as? ScanResult

        if (imageUriString != null) {
            val uri = imageUriString.toUri()
            binding.ivCapturedImage.load(uri)

            if (scanResult != null) {
                displayResult(scanResult)
            } else if (uri.scheme == "file") {
                val file = File(uri.path!!)
                viewModel.scanImage(file)
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
                    Toast.makeText(
                        context,
                        getString(R.string.scanning_error_prefix, error.message.orEmpty()),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun displayResult(scanResult: ScanResult) {
        currentScanResult = scanResult

        val displayCategory = ScannedCategoryHelper.toDisplayCategory(scanResult.category)
        val isNotSure = ScannedCategoryHelper.isUncertain(scanResult.category)
        val isTextile = ScannedCategoryHelper.isTextileCategory(scanResult.category)
        val effectiveRecyclable = ScannedCategoryHelper.isCategoryRecyclable(scanResult.category)

        binding.tvCategory.text = displayCategory

        if (isNotSure) {
            binding.tvBadge.text = getString(R.string.scanning_status_not_sure)
            binding.tvBadge.setTextColor(
                androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.holo_orange_dark)
            )
            binding.tvBadge.setBackgroundResource(R.drawable.bg_badge_recyclable)

            binding.ivSuccess.setImageResource(R.drawable.ic_help_24)
            binding.ivSuccess.setColorFilter(
                androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.holo_orange_dark)
            )

            binding.tvDescriptionWait.text = getString(R.string.scanning_desc_not_sure)
            binding.tvDescriptionWait.setTextColor(
                androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.holo_orange_dark)
            )
        } else if (effectiveRecyclable) {
            binding.tvBadge.text = getString(R.string.scanning_status_recyclable)
            binding.tvBadge.setTextColor(
                androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark)
            )
            binding.tvBadge.setBackgroundResource(R.drawable.bg_badge_recyclable)

            binding.ivSuccess.setImageResource(R.drawable.ic_check_circle_24)
            binding.ivSuccess.setColorFilter(
                androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark)
            )

            binding.tvDescriptionWait.text = getString(R.string.scanning_desc_recyclable)
            binding.tvDescriptionWait.setTextColor(
                androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark)
            )
        } else {
            binding.tvBadge.text = getString(R.string.scanning_status_not_recyclable)
            binding.tvBadge.setTextColor(
                androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)
            )
            binding.tvBadge.setBackgroundResource(R.drawable.bg_badge_recyclable)

            binding.ivSuccess.setImageResource(R.drawable.ic_close_24)
            binding.ivSuccess.setColorFilter(
                androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)
            )

            binding.tvDescriptionWait.text = getString(R.string.scanning_desc_not_recyclable)
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

        binding.tvInstructionTitle.text = getString(R.string.scanning_instruction_title)
        binding.tvInstructionSteps.text = if (steps.isNotEmpty()) {
            steps.mapIndexed { index, tip -> "${index + 1}. $tip" }.joinToString("\n")
        } else {
            getString(R.string.scanning_instruction_default)
        }

        val canProceedToRecycleFlow = effectiveRecyclable && !isNotSure && !isTextile
        updateRecycleButtonState(canProceedToRecycleFlow, textileDisabled = isTextile && effectiveRecyclable)
    }

    private fun setupListeners() {
        binding.btnScanAgain.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnNotNow.setOnClickListener {
            findNavController().popBackStack(R.id.nav_home, false)
        }

        binding.btnRecycle.setOnClickListener {
            if (!binding.btnRecycle.isEnabled) {
                return@setOnClickListener
            }

            val rawCategory = currentScanResult?.category.orEmpty()
            val effectiveRecyclable = ScannedCategoryHelper.isCategoryRecyclable(rawCategory)
            val mappedWasteCategory = ScannedCategoryHelper.toCheckInWasteType(rawCategory)
            val selectedBinType = ScannedCategoryHelper.toBinType(rawCategory, effectiveRecyclable)

            Log.d("ScanningResult", "Passing wasteCategory: $rawCategory")
            Log.d("ScanningResult", "Passing mappedWasteCategory: $mappedWasteCategory")
            Log.d("ScanningResult", "Passing selectedBinType: $selectedBinType")

            val bundle = Bundle().apply {
                putString("selectedBinType", selectedBinType)
                putString("wasteCategory", rawCategory)
                putString("mappedWasteCategory", mappedWasteCategory)
            }

            findNavController().navigate(
                R.id.action_scanningResultFragment_to_nearbyBinFragment,
                bundle
            )
        }

    }

    private fun updateRecycleButtonState(enabled: Boolean, textileDisabled: Boolean = false) {
        binding.btnRecycle.isEnabled = enabled
        binding.btnRecycle.isClickable = enabled
        binding.btnRecycle.alpha = if (enabled) 1f else 0.55f
        binding.btnRecycle.text = if (enabled) {
            getString(R.string.scanning_recycle_cta_enabled)
        } else if (textileDisabled) {
            getString(R.string.scanning_recycle_cta_textile_disabled)
        } else {
            getString(R.string.scanning_recycle_cta_disabled)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
