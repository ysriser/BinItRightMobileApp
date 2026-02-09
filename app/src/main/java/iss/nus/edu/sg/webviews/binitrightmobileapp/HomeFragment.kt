package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import iss.nus.edu.sg.webviews.binitrightmobileapp.databinding.FragmentHomeBinding
import iss.nus.edu.sg.webviews.binitrightmobileapp.network.RetrofitClient
import kotlinx.coroutines.launch

class HomeFragment : Fragment(R.layout.fragment_home) {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)

        binding.btnScan.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_scanItem)
        }

        binding.btnQuiz.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_questionnaire)
        }

        binding.cardFindBins.setOnClickListener {
            navigateToFindBins()
        }

        binding.btnRecycleNow.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_scanHome)
        }

        binding.cardChatHelper.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_chatFragment)
        }

        binding.cardAchievements.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_achievements)
        }

        setupReportIssueButton()
        fetchUserStats()
    }

    private fun navigateToFindBins() {
        findNavController().navigate(R.id.action_home_to_findRecyclingBinFragment)
    }

    private fun setupReportIssueButton() {
        binding.cardReportIssue.setOnClickListener {
            ReportIssueDialogFragment().show(childFragmentManager, "ReportIssue")
        }
    }

    private fun fetchUserStats() {
        val userId = requireActivity()
            .getSharedPreferences("APP_PREFS", Context.MODE_PRIVATE)
            .getLong("USER_ID", -1L)

        if (userId == -1L) {
            Log.e("HomeFragment", "USER_ID is -1, skip profile summary API call.")
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService().getProfileSummary()
                if (response.isSuccessful && response.body() != null) {
                    val user = response.body()!!
                    binding.tvPointsCount.text = user.pointBalance.toString()
                    binding.tvRecycledCount.text = user.totalRecycled.toString()
                    binding.tvAchievementCount.text = user.totalAchievement.toString()
                    binding.tvCo2Saved.text = String.format("%.1f kg", user.carbonEmissionSaved)
                    binding.aiSummary.text = user.aiSummary
                }
            } catch (error: Exception) {
                Log.e("HomeFragment", "Network error while loading profile summary: ${error.message}", error)
                binding.aiSummary.text = "You're making a positive environmental impact. Keep recycling!"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

