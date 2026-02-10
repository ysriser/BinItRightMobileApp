package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.graphics.Color
import android.os.Bundle
import android.util.Log
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

    private var currentOutcome: SerializableOutcome? = null

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
        val specialRecyclable = ScannedCategoryHelper.isSpecialRecyclable(outcome.categoryTitle)
        val fromOutcome = outcome.disposalLabel.equals("Recyclable", ignoreCase = true)
        val effectiveRecyclable = fromOutcome || specialRecyclable

        binding.tvCategory.text = displayCategory

        val tipsText = if (!outcome.instruction.isNullOrBlank()) {
            outcome.instruction
        } else {
            outcome.tips.mapIndexed { index, tip -> "${index + 1}. $tip" }.joinToString("\n")
        }
        binding.tvTips.text = tipsText

        binding.tvExplanation.text = outcome.explanation
        binding.tvExplanationDetail.text = "Certainty: ${outcome.certainty}"

        if (uncertain) {
            binding.tvBadge.text = "Not sure"
            binding.tvBadge.background.setTint(Color.parseColor("#FFF3E0"))
            binding.tvBadge.setTextColor(Color.parseColor("#EF6C00"))
            binding.ivSuccess.setImageResource(R.drawable.ic_help_24)
            binding.ivSuccess.setColorFilter(Color.parseColor("#EF6C00"))
            binding.cardInfo.setCardBackgroundColor(Color.parseColor("#FFF8E1"))
            binding.tvExplanation.setTextColor(Color.parseColor("#E65100"))
            binding.tvExplanationDetail.setTextColor(Color.parseColor("#EF6C00"))
        } else if (effectiveRecyclable) {
            binding.tvBadge.text = "Recyclable"
            binding.tvBadge.background.setTint(Color.parseColor("#E8F5E9"))
            binding.tvBadge.setTextColor(Color.parseColor("#00695C"))
            binding.ivSuccess.setImageResource(R.drawable.ic_check_circle_24)
            binding.ivSuccess.setColorFilter(Color.parseColor("#00C853"))
            binding.cardInfo.setCardBackgroundColor(Color.parseColor("#E8F5E9"))
            binding.tvExplanation.setTextColor(Color.parseColor("#1B5E20"))
            binding.tvExplanationDetail.setTextColor(Color.parseColor("#2E7D32"))
        } else {
            binding.tvBadge.text = "Not Recyclable"
            binding.tvBadge.background.setTint(Color.parseColor("#FFEBEE"))
            binding.tvBadge.setTextColor(Color.parseColor("#C62828"))
            binding.ivSuccess.setImageResource(R.drawable.ic_error_24)
            binding.ivSuccess.setColorFilter(Color.parseColor("#D32F2F"))
            binding.cardInfo.setCardBackgroundColor(Color.parseColor("#FFEBEE"))
            binding.tvExplanation.setTextColor(Color.parseColor("#B71C1C"))
            binding.tvExplanationDetail.setTextColor(Color.parseColor("#C62828"))
        }
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
            val outcome = currentOutcome
            val category = outcome?.categoryTitle.orEmpty()
            val effectiveRecyclable = outcome?.disposalLabel.equals("Recyclable", ignoreCase = true)
                    || ScannedCategoryHelper.isSpecialRecyclable(category)
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
}