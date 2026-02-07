package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import iss.nus.edu.sg.webviews.binitrightmobileapp.databinding.FragmentQuestionnaireResultBinding

class QuestionnaireResultFragment : Fragment() {

    private var _binding: FragmentQuestionnaireResultBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuestionnaireResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val outcome = arguments?.getSerializable("outcome") as? SerializableOutcome
        if (outcome != null) {
            setupUI(outcome)
        } else {
            Toast.makeText(context, "Error loading result", Toast.LENGTH_SHORT).show()
        }

        setupListeners()
    }

    private fun setupUI(outcome: SerializableOutcome) {
        binding.tvCategory.text = outcome.categoryTitle
        binding.tvExplanation.text = outcome.explanation
        binding.tvExplanationDetail.text = "Certainty: ${outcome.certainty}"

        if (!outcome.instruction.isNullOrBlank()) {
            binding.tvTips.text = outcome.instruction
        } else {
            val tipsText = outcome.tips
                .mapIndexed { index, tip -> "${index + 1}. $tip" }
                .joinToString("\n")
            binding.tvTips.text = tipsText
        }

        val certainty = try {
            Certainty.valueOf(outcome.certainty)
        } catch (e: Exception) {
            Certainty.LOW
        }

        val recyclableFromOutcome = outcome.disposalLabel.equals("Recyclable", ignoreCase = true)
        val isRecyclable = WasteCategoryMapper.shouldDisplayAsRecyclable(
            outcome.categoryTitle,
            recyclableFromOutcome
        )
        val isNotSure = isNotSureCategory(outcome.categoryTitle) ||
            outcome.disposalLabel.equals("Not sure", ignoreCase = true)

        if (isNotSure) {
            binding.tvBadge.text = "Not sure"
            binding.tvBadge.setBackgroundResource(R.drawable.bg_badge_recyclable)
            binding.tvBadge.background.setTint(Color.parseColor("#FFF3E0"))
            binding.tvBadge.setTextColor(Color.parseColor("#E65100"))
            binding.ivSuccess.setImageResource(R.drawable.ic_help_24)
            binding.ivSuccess.setColorFilter(Color.parseColor("#E65100"))
        } else if (isRecyclable) {
            binding.tvBadge.text = "Recyclable"
            binding.tvBadge.setBackgroundResource(R.drawable.bg_badge_recyclable)
            binding.tvBadge.background.setTint(Color.parseColor("#E8F5E9"))
            binding.tvBadge.setTextColor(Color.parseColor("#00695C"))
            binding.ivSuccess.setImageResource(R.drawable.ic_check_circle_24)
            binding.ivSuccess.setColorFilter(Color.parseColor("#00C853"))
        } else {
            binding.tvBadge.text = "Not Recyclable"
            binding.tvBadge.setBackgroundResource(R.drawable.bg_badge_recyclable)
            binding.tvBadge.background.setTint(Color.parseColor("#EEEEEE"))
            binding.tvBadge.setTextColor(Color.parseColor("#616161"))
            binding.ivSuccess.setImageResource(R.drawable.ic_error_24)
            binding.ivSuccess.setColorFilter(Color.parseColor("#D32F2F"))
        }

        when (certainty) {
            Certainty.HIGH -> {
                binding.cardInfo.setCardBackgroundColor(Color.parseColor("#E8F5E9"))
                binding.tvExplanation.setTextColor(Color.parseColor("#1B5E20"))
                binding.tvExplanationDetail.setTextColor(Color.parseColor("#2E7D32"))
            }
            Certainty.MEDIUM -> {
                binding.cardInfo.setCardBackgroundColor(Color.parseColor("#E3F2FD"))
                binding.tvExplanation.setTextColor(Color.parseColor("#0D47A1"))
                binding.tvExplanationDetail.setTextColor(Color.parseColor("#1565C0"))
            }
            Certainty.LOW -> {
                binding.cardInfo.setCardBackgroundColor(Color.parseColor("#FFFDE7"))
                binding.tvExplanation.setTextColor(Color.parseColor("#F57F17"))
                binding.tvExplanationDetail.setTextColor(Color.parseColor("#F9A825"))
            }
        }
    }

    private fun setupListeners() {
        binding.btnScanAgain.text = "Do another questionnaire?"
        binding.btnScanAgain.setOnClickListener {
            val navController = findNavController()
            val popped = navController.popBackStack(R.id.questionnaireFragment, false)
            if (!popped) {
                navController.navigate(R.id.questionnaireFragment)
            }
        }
        binding.btnScanAgain.setIconResource(R.drawable.ic_refresh_24)

        binding.btnTryAiScan.setOnClickListener {
            findNavController().navigate(R.id.action_questionnaireResultFragment_to_scanItemFragment)
        }

        binding.btnNotNow.setOnClickListener {
            findNavController().popBackStack(R.id.nav_home, false)
        }

        binding.btnRecycle.setOnClickListener {
            val category = binding.tvCategory.text.toString()
            val mappedWasteType = WasteCategoryMapper.mapCategoryToWasteType(category)
            val mappedBinType = WasteCategoryMapper.mapWasteTypeToBinType(mappedWasteType)

            val bundle = Bundle().apply {
                putString("selectedBinType", mappedBinType)
                putString("wasteCategory", mappedWasteType)
                putString("scannedCategory", category)
            }

            findNavController().navigate(
                R.id.action_questionnaireResultFragment_to_nearbyBinFragment,
                bundle
            )
        }

        binding.btnAccurate.setOnClickListener {
            Toast.makeText(context, "Thanks for feedback!", Toast.LENGTH_SHORT).show()
            binding.cardAccuracy.isVisible = false
        }

        binding.btnIncorrect.setOnClickListener {
            Toast.makeText(context, "Thanks! We'll improve.", Toast.LENGTH_SHORT).show()
            binding.cardAccuracy.isVisible = false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun isNotSureCategory(category: String): Boolean {
        val normalized = category.trim().lowercase()
        return normalized.contains("not sure") ||
            normalized.contains("uncertain") ||
            normalized.contains("unknown") ||
            normalized.contains("other")
    }
}
