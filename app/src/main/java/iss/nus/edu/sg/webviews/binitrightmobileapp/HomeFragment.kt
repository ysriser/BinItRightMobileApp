package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.os.Bundle
import android.content.Context
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions

import androidx.navigation.fragment.findNavController
import iss.nus.edu.sg.webviews.binitrightmobileapp.databinding.FragmentHomeBinding

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

        // Logout listener MUST be inside onViewCreated
        binding.btnLogout.setOnClickListener {
            handleLogout()
        }
    }

    private fun handleLogout() {
        // 1. Clear the session token (Consider using a dedicated SessionManager class later!)
        val prefs = requireContext().getSharedPreferences("APP_PREFS", Context.MODE_PRIVATE)
        prefs.edit().remove("TOKEN").apply()

        // 2. Navigate using the Action ID defined in your XML
        // This will automatically trigger the popUpTo logic you wrote in the XML
        findNavController().navigate(R.id.action_nav_home_to_loginFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}