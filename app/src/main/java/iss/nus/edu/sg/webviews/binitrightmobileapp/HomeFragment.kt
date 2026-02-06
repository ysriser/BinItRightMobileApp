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

    companion object {
        private const val TAG = "HomeFragment"
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)

        binding.btnScan.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_scanHome)
        }

        binding.btnQuiz.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_questionnaire)
        }

        binding.cardFindBins.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_findRecyclingBinFragment)
        }

        binding.btnRecycleNow.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_scanHome)
        }


        binding.cardAchievements.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_achievements)
        }

        setupReportIssueButton()

        fetchUserStats()
    }

    private fun setupReportIssueButton() {
        val reportIssueCard = view?.findViewById<View>(R.id.cardReportIssue)
        reportIssueCard?.setOnClickListener {
            ReportIssueDialogFragment().show(childFragmentManager, "ReportIssue")
        }
    }

    private fun fetchUserStats() {
        val userId = requireActivity()
            .getSharedPreferences("APP_PREFS", Context.MODE_PRIVATE)
            .getLong("USER_ID", -1L)

        if (userId != -1L) {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val response = RetrofitClient.apiService().getUserProfile(userId)
                    if (response.isSuccessful && response.body() != null) {
                        val user = response.body()!!
                        binding.tvPointsCount.text = user.pointBalance.toString()
                        Log.d(TAG, "###Point: ${user.pointBalance}")
                    } else {
                        Log.e(TAG, "###Server Error: ${response.code()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "###Network Crash: ${e.message}", e)
                }
            }
        } else {
            Log.d(TAG, "User not logged in (UserId = -1)")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
