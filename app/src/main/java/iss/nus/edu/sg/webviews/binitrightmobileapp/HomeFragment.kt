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
        // 1. Clear the session token
        val prefs = requireContext().getSharedPreferences("APP_PREFS", Context.MODE_PRIVATE)
        prefs.edit().remove("TOKEN").apply()

        // 2. Navigate back to Login and clear backstack
        // The popUpTo ID should be the ID of the home fragment's destination in the nav graph
        val navOptions = NavOptions.Builder()
            // In NavOptions, you refer to the fragment's own ID, not an action ID.
            // Let's assume the ID in your nav_graph.xml is 'homeFragment'.
            .setPopUpTo(R.id.homeFragment, true)
            .build()

        // The navigation action ID from home to login.
        // This must match an <action> tag in your nav_graph.xml
        findNavController().navigate(R.id.action_homeFragment_to_loginFragment, null, navOptions)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}