package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.graphics.toColorInt
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import iss.nus.edu.sg.webviews.binitrightmobileapp.databinding.FragmentQuestionnaireResultBinding
import java.util.Locale

class QuestionnaireResultFragment : Fragment() {

    private var _binding: FragmentQuestionnaireResultBinding? = null
    private val binding get() = _binding!!

    private var currentOutcome: SerializableOutcome? = null
    private var canProceedToRecycleFlow: Boolean = false

    companion object {
        private const val RECYCLABLE_LABEL = "recyclable"
        private const val COLOR_UNCERTAIN_ACCENT = "#EF6C00"
    }

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
            currentOutcome = outcome
            setupUI(outcome)
        } else {
            Toast.makeText(context, "Error loading result", Toast.LENGTH_SHORT).show()
        }

        setupListeners()
    }

    private fun setupUI(outcome: SerializableOutcome) {
        val displayCategory = ScannedCategoryHelper.toDisplayCategory(outcome.categoryTitle)
        val uncertain = ScannedCategoryHelper.isUncertain(outcome.categoryTitle)
        val textile = ScannedCategoryHelper.isTextileCategory(outcome.categoryTitle)
        val fromOutcome = isRecyclableLabel(outcome.disposalLabel)
        val categoryAllowsRecycle = ScannedCategoryHelper.isCategoryRecyclable(outcome.categoryTitle)
        val effectiveRecyclable = if (textile) true else fromOutcome && categoryAllowsRecycle

        binding.tvCategory.text = displayCategory

        val tipsText = if (!outcome.instruction.isNullOrBlank()) {
            outcome.instruction
        } else {
            outcome.tips.mapIndexed { index, tip -> "${index + 1}. $tip" }.joinToString("\n")
        }
        binding.tvTips.text = tipsText

        binding.tvExplanation.text = outcome.explanation
        binding.tvExplanationDetail.text = getString(
            R.string.questionnaire_certainty_format,
            outcome.certainty
        )

        if (uncertain) {
            binding.tvBadge.text = getString(R.string.questionnaire_status_not_sure)
            binding.tvBadge.background.setTint("#FFF3E0".toColorInt())
            binding.tvBadge.setTextColor(COLOR_UNCERTAIN_ACCENT.toColorInt())
            binding.ivSuccess.setImageResource(R.drawable.ic_help_24)
            binding.ivSuccess.setColorFilter(COLOR_UNCERTAIN_ACCENT.toColorInt())
            binding.cardInfo.setCardBackgroundColor("#FFF8E1".toColorInt())
            binding.tvExplanation.setTextColor("#E65100".toColorInt())
            binding.tvExplanationDetail.setTextColor(COLOR_UNCERTAIN_ACCENT.toColorInt())
        } else if (effectiveRecyclable) {
            binding.tvBadge.text = getString(R.string.questionnaire_status_recyclable)
            binding.tvBadge.background.setTint("#E8F5E9".toColorInt())
            binding.tvBadge.setTextColor("#00695C".toColorInt())
            binding.ivSuccess.setImageResource(R.drawable.ic_check_circle_24)
            binding.ivSuccess.setColorFilter("#00C853".toColorInt())
            binding.cardInfo.setCardBackgroundColor("#E8F5E9".toColorInt())
            binding.tvExplanation.setTextColor("#1B5E20".toColorInt())
            binding.tvExplanationDetail.setTextColor("#2E7D32".toColorInt())
        } else {
            binding.tvBadge.text = getString(R.string.questionnaire_status_not_recyclable)
            binding.tvBadge.background.setTint("#FFEBEE".toColorInt())
            binding.tvBadge.setTextColor("#C62828".toColorInt())
            binding.ivSuccess.setImageResource(R.drawable.ic_error_24)
            binding.ivSuccess.setColorFilter("#D32F2F".toColorInt())
            binding.cardInfo.setCardBackgroundColor("#FFEBEE".toColorInt())
            binding.tvExplanation.setTextColor("#B71C1C".toColorInt())
            binding.tvExplanationDetail.setTextColor("#C62828".toColorInt())
        }

        canProceedToRecycleFlow = effectiveRecyclable && !uncertain && !textile
        updateRecycleButtonState(
            canProceedToRecycleFlow,
            textileDisabled = textile && effectiveRecyclable
        )
    }

    private fun setupListeners() {
        binding.btnScanAgain.setOnClickListener {
            val navController = findNavController()
            val popped = navController.popBackStack(R.id.questionnaireFragment, false)
            if (!popped) {
                navController.navigate(R.id.questionnaireFragment)
            }
        }

        binding.btnTryAiScan.setOnClickListener {
            findNavController().navigate(R.id.action_questionnaireResultFragment_to_scanItemFragment)
        }

        binding.btnNotNow.setOnClickListener {
            findNavController().popBackStack(R.id.nav_home, false)
        }

        binding.btnRecycle.setOnClickListener {
            if (!canProceedToRecycleFlow || !binding.btnRecycle.isEnabled) {
                return@setOnClickListener
            }
            val outcome = currentOutcome
            val category = outcome?.categoryTitle.orEmpty()
            val effectiveRecyclable =
                isRecyclableLabel(outcome?.disposalLabel) && ScannedCategoryHelper.isCategoryRecyclable(category)
            val mappedWasteCategory = ScannedCategoryHelper.toCheckInWasteType(category)
            val selectedBinType = ScannedCategoryHelper.toBinType(category, effectiveRecyclable)

            val bundle = Bundle().apply {
                putString("selectedBinType", selectedBinType)
                putString("wasteCategory", category)
                putString("mappedWasteCategory", mappedWasteCategory)
            }

            Log.d("QuestionnaireResult", "Passing selectedBinType: $selectedBinType")
            Log.d("QuestionnaireResult", "Passing wasteCategory: $category")
            Log.d("QuestionnaireResult", "Passing mappedWasteCategory: $mappedWasteCategory")

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

    private fun isRecyclableLabel(disposalLabel: String?): Boolean {
        return disposalLabel
            ?.trim()
            ?.lowercase(Locale.ROOT) == RECYCLABLE_LABEL
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
}
