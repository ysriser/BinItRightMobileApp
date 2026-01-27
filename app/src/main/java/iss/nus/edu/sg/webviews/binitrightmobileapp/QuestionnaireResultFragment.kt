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
        binding.tvExplanationDetail.text = "Certainty: ${outcome.certainty}" // Added certainty info
        
        // Tips
        val tipsText = outcome.tips.joinToString("\n") { "• $it" }
        binding.tvTips.text = tipsText

        // Logic based on Certainty and Label
        val certainty = try { Certainty.valueOf(outcome.certainty) } catch (e: Exception) { Certainty.LOW }
        val isRecyclable = outcome.disposalLabel.equals("Recyclable", ignoreCase = true)
        
        // Reset Icon/Badge to base logic (Type-based) first
        if (isRecyclable) {
            binding.tvBadge.setBackgroundResource(R.drawable.bg_badge_recyclable) 
            binding.tvBadge.background.setTint(Color.parseColor("#E8F5E9"))
            binding.tvBadge.setTextColor(Color.parseColor("#00695C"))
            binding.ivSuccess.setImageResource(R.drawable.ic_check_circle_24)
            binding.ivSuccess.setColorFilter(Color.parseColor("#00C853"))
        } else {
             // Default for non-recyclable or unsure
             binding.tvBadge.setBackgroundResource(R.drawable.bg_badge_recyclable)
             binding.tvBadge.background.setTint(Color.parseColor("#EEEEEE"))
             binding.tvBadge.setTextColor(Color.parseColor("#616161"))
             binding.ivSuccess.setImageResource(R.drawable.ic_help_24)
             binding.ivSuccess.setColorFilter(Color.parseColor("#757575"))
             
             if (!outcome.disposalLabel.equals("Not sure", ignoreCase = true)) {
                 // Explicit non-recyclable/special
                 binding.ivSuccess.setImageResource(R.drawable.ic_error_24)
                 binding.ivSuccess.setColorFilter(Color.parseColor("#D32F2F"))
             }
        }

        // Apply Color Coding to INFO CARD AREA (Background & Texts)
        when (certainty) {
            Certainty.HIGH -> {
                if (isRecyclable) {
                    // High Certainty Recyclable -> Green Card
                     binding.cardInfo.setCardBackgroundColor(Color.parseColor("#E8F5E9")) // Light Green
                     binding.tvExplanation.setTextColor(Color.parseColor("#1B5E20"))
                     binding.tvExplanationDetail.setTextColor(Color.parseColor("#2E7D32"))
                } else {
                    // High Certainty Non-Recyclable -> Same Green Card
                    binding.cardInfo.setCardBackgroundColor(Color.parseColor("#E8F5E9")) // Light Green
                    binding.tvExplanation.setTextColor(Color.parseColor("#1B5E20"))
                    binding.tvExplanationDetail.setTextColor(Color.parseColor("#2E7D32"))
                }
            }
            Certainty.MEDIUM -> {
                // Medium -> Blue Card
                 binding.cardInfo.setCardBackgroundColor(Color.parseColor("#E3F2FD")) // Light Blue
                 binding.tvExplanation.setTextColor(Color.parseColor("#0D47A1"))
                 binding.tvExplanationDetail.setTextColor(Color.parseColor("#1565C0"))
            }
            Certainty.LOW -> {
                // Low -> Yellow Card
                 binding.cardInfo.setCardBackgroundColor(Color.parseColor("#FFFDE7")) // Light Yellow
                 binding.tvExplanation.setTextColor(Color.parseColor("#F57F17")) // Darker Yellow text for contrast
                 binding.tvExplanationDetail.setTextColor(Color.parseColor("#F9A825"))
            }
        }
    }

    private fun setupListeners() {
        binding.btnScanAgain.text = "Do another questionnaire?"
        binding.btnScanAgain.setOnClickListener {
             // Pop back to start of questionnaire
             findNavController().popBackStack(R.id.homeFragment, false)
             findNavController().navigate(R.id.action_homeFragment_to_questionnaireFragment)
        }
        binding.btnScanAgain.setIconResource(R.drawable.ic_refresh_24)
        
        binding.btnNotNow.setOnClickListener {
            findNavController().popBackStack(R.id.homeFragment, false)
        }

        binding.btnRecycle.setOnClickListener {
             Toast.makeText(context, "Proceed to drop-off guidance (TODO)", Toast.LENGTH_SHORT).show()
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
