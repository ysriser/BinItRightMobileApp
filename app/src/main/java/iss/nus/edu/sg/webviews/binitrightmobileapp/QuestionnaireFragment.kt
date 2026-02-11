package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import iss.nus.edu.sg.webviews.binitrightmobileapp.databinding.FragmentQuestionnaireBinding
import iss.nus.edu.sg.webviews.binitrightmobileapp.databinding.RowAnswerBinding

class QuestionnaireFragment : Fragment() {

    private var _binding: FragmentQuestionnaireBinding? = null
    private val binding get() = _binding!!

    private val viewModel: QuestionnaireViewModel by viewModels()
    private lateinit var optionAdapter: QuestionnaireOptionAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuestionnaireBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObservers()
        setupListeners()
        setupBackHandling()
    }

    private fun setupRecyclerView() {
        optionAdapter = QuestionnaireOptionAdapter { option ->
            if (option.id == "BACK_ACTION") {
                // Animate for Back: Slide Current to Right (+100)
                isBackNavigation = true // Flag for next observer update
                binding.contentContainer.animate()
                    .alpha(0f)
                    .translationX(100f)
                    .setDuration(150)
                    .withEndAction {
                        viewModel.selectOption(option.id)
                    }
                    .start()
            } else {
                // Animate for Forward: Slide Current to Left (-100)
                binding.contentContainer.animate()
                    .alpha(0f)
                    .translationX(-100f)
                    .setDuration(150)
                    .withEndAction {
                        viewModel.selectOption(option.id)
                    }
                    .start()
            }
        }
        binding.rvOptions.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = optionAdapter
            itemAnimator = null 
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            handleBack()
        }
    }

    private fun setupBackHandling() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBack()
            }
        })
    }

    private fun handleBack() {
        // Animate Out (Right)
        binding.contentContainer.animate()
            .alpha(0f)
            .translationX(100f) // Move right
            .setDuration(150)
            .withEndAction {
                if (!viewModel.navigateBack()) {
                     findNavController().popBackStack()
                } else {
                     isBackNavigation = true
                }
            }
            .start()
    }

    private var isBackNavigation = false
    private var lastProgressValue = 0

    private fun setupObservers() {
        // We need to coordinate the update with animation. 
        // Simply observing 'currentQuestion' updates immediately. 
        // We'll use a local flag or custom observer logic.

        viewModel.currentQuestion.observe(viewLifecycleOwner) { question ->
            if (question != null) {
                 updateQuestionContent(question)
                 
                 // Animation Logic
                 if (isBackNavigation) {
                     // Coming back: Enter Keyframe - Start from Left (-X), Slide to 0
                     binding.contentContainer.translationX = -100f
                     isBackNavigation = false // Reset
                 } else {
                     // Moving forward: Enter Keyframe - Start from Right (+X), Slide to 0
                      binding.contentContainer.translationX = 100f
                 }
                 binding.contentContainer.alpha = 0f
                 binding.contentContainer.animate()
                    .alpha(1f)
                    .translationX(0f)
                    .setDuration(200)
                    .start()
            }
        }
        
        viewModel.currentProgress.observe(viewLifecycleOwner) { (current, _) ->
            binding.tvProgressLabel.text = getString(R.string.question_progress_label, current)
            val progress = (current.toFloat() / 5f * 100).toInt().coerceAtMost(100)
            
            // Smooth Animation for Progress Bar
            ObjectAnimator.ofInt(binding.progressBar, "progress", progress)
                .setDuration(500)
                .start()
        }

        viewModel.answersSummary.observe(viewLifecycleOwner) { summary ->
            binding.tvSummaryTitle.isVisible = summary.isNotEmpty()
            binding.layoutAnswersSummary.isVisible = summary.isNotEmpty()
            binding.layoutAnswersSummary.removeAllViews()
            
            summary.forEach { (_, answerText) ->
                val rowBinding = RowAnswerBinding.inflate(LayoutInflater.from(context), binding.layoutAnswersSummary, true)
                rowBinding.tvAnswerText.text = answerText
            }
        }

        viewModel.navigationEvent.observe(viewLifecycleOwner) { nextId ->
            if (nextId != null) {
                val outcome = viewModel.getOutcome(nextId)
                if (outcome != null) {
                    navigateToResult(outcome)
                }
                viewModel.consumeNavigation()
            }
        }
    }
    
    private fun updateQuestionContent(question: QuestionNode) {
        binding.tvHeaderTitle.text = question.title ?: getString(R.string.questionnaire_header_title)
        binding.tvQuestion.text = question.question
        binding.tvSubtitle.text = question.subtitle
        binding.tvSubtitle.isVisible = !question.subtitle.isNullOrEmpty()
        optionAdapter.submitList(question.options)
    }


    private fun navigateToResult(outcome: OutcomeNode) {
        val bundle = Bundle().apply {
            putSerializable("outcome", SerializableOutcome(
                categoryTitle = outcome.categoryTitle,
                disposalLabel = outcome.disposalLabel,
                certainty = outcome.certainty,
                explanation = outcome.explanation,
                tips = outcome.tips
            )
            )
        }
        findNavController().navigate(R.id.action_questionnaireFragment_to_questionnaireResultFragment, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
