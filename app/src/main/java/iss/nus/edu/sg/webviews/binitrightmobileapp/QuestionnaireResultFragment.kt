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
        inflater: LayoutInflater, container: ViewGroup?,
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
        binding.tvBadge.text = outcome.disposalLabel
        binding.tvExplanation.text = outcome.explanation
        binding.tvExplanationDetail.text = "Certainty: ${outcome.certainty}"

        if (!outcome.instruction.isNullOrBlank()) {
            binding.tvTips.text = outcome.instruction
        } else {
            val tipsText = outcome.tips.mapIndexed { index, tip -> "${index + 1}. $tip" }.joinToString("\n")
            binding.tvTips.text = tipsText
        }

        val isRecyclable = outcome.disposalLabel.equals("Recyclable", ignoreCase = true)

        if (isRecyclable) {
            binding.tvBadge.setBackgroundResource(R.drawable.bg_badge_recyclable)
            binding.tvBadge.background.setTint(Color.parseColor("#E8F5E9"))
            binding.tvBadge.setTextColor(Color.parseColor("#00695C"))
            binding.ivSuccess.setImageResource(R.drawable.ic_check_circle_24)
            binding.ivSuccess.setColorFilter(Color.parseColor("#00C853"))
        } else {
            binding.tvBadge.setBackgroundResource(R.drawable.bg_badge_recyclable)
            binding.tvBadge.background.setTint(Color.parseColor("#EEEEEE"))
            binding.tvBadge.setTextColor(Color.parseColor("#616161"))
            binding.ivSuccess.setImageResource(R.drawable.ic_help_24)
            binding.ivSuccess.setColorFilter(Color.parseColor("#757575"))

            if (!outcome.disposalLabel.equals("Not sure", ignoreCase = true)) {
                binding.ivSuccess.setImageResource(R.drawable.ic_error_24)
                binding.ivSuccess.setColorFilter(Color.parseColor("#D32F2F"))
            }
        }

        val certainty = try { Certainty.valueOf(outcome.certainty) } catch (e: Exception) { Certainty.LOW }
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
             // 修复：先退回到 scanHomeFragment，然后再通过正确的 Action 跳转到 questionnaire
             // 由于 questionnaireFragment 只能从 scanHomeFragment 进入
             findNavController().popBackStack(R.id.scanHomeFragment, false)
        }
        binding.btnScanAgain.setIconResource(R.drawable.ic_refresh_24)

        binding.btnNotNow.setOnClickListener {
            findNavController().popBackStack(R.id.nav_home, false)
        }

        binding.btnRecycle.setOnClickListener {
            val itemType = binding.tvCategory.text.toString()
            val binType = when (itemType.uppercase()) {
                "RECYCLABLE ITEM" -> "BlueBin"
                "ELECTRONIC", "ELECTRONICS", "E-WASTE", "EWASTE" -> "EWaste"
                "LIGHTING", "LAMP", "LIGHT", "BULB" -> "Lamp"
                else -> ""
            }
            val bundle = Bundle().apply {
                putString("selectedBinType", binType)
                putString("wasteCategory", itemType)
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
}