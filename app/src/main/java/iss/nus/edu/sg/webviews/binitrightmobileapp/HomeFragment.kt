package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.content.ContentValues.TAG
import android.os.Bundle
import android.content.Context
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions

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

        // Existing Navigation listeners
        binding.btnScan.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_scanItem)
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
        // Assuming you store the logged-in user ID in SharedPreferences
        val userId = requireActivity()
            .getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
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
                        // This will tell you if you got a 404, 500, etc.
                        Log.e(TAG, "###Server Error: ${response.code()} - ${response.errorBody()?.string()}")
                    }
                } catch (e: Exception) {
                    // This will tell you if it's a connection timeout or URL crash
                    Log.e(TAG, "###Network Crash: ${e.message}", e)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}