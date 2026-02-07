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

        binding.cardChatHelper.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_chatFragment)
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

        Log.d(TAG, "###Fetching stats for userId: $userId") // Add this

        if (userId != -1L) {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    Log.d(TAG, "###Making API call to getUserProfile($userId)") // Add this
                    val response = RetrofitClient.apiService().getProfileSummary()
                    Log.d(TAG, "###Response received - Code: ${response.code()}, Successful: ${response.isSuccessful}") // Add this

                    if (response.isSuccessful && response.body() != null) {
                        val user = response.body()!!
                        Log.d(TAG, "###Point Balance from API: ${user.pointBalance}") // Add this
                        Log.d(TAG, "###Setting text to: ${user.pointBalance}") // Add this
                        binding.tvPointsCount.text = user.pointBalance.toString()
                        binding.tvRecycledCount.text = user.totalRecycled.toString()
                        Log.d(TAG, "###Text set successfully") // Add this
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e(TAG, "###Server Error: ${response.code()} - $errorBody")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "###Network Crash: ${e.message}", e)
                    e.printStackTrace()
                }
            }
        } else {
            Log.e(TAG, "###USER_ID is -1, not making API call")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}