package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.os.Bundle
import android.content.Context
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import iss.nus.edu.sg.webviews.binitrightmobileapp.databinding.FragmentHomeBinding

class HomeFragment : Fragment(R.layout.fragment_home) {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // Initialize the ViewModel to access achievement data
    private val achievementViewModel: AchievementViewModel by viewModels()

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
            findNavController().navigate(R.id.action_home_to_findRecyclingBinFragment)
        }

        binding.btnRecycleNow.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_scanHome)
        }

        // Updated ID to match XML: cardAchievements
        binding.cardAchievements.setOnClickListener {
            findNavController().navigate(R.id.achievementsFragment)
        }

        binding.cardNextMilestone.setOnClickListener {
            findNavController().navigate(R.id.achievementsFragment)
        }

        binding.btnLogout.setOnClickListener {
            handleLogout()
        }

        // Observe achievement list to update the count on the home screen
        achievementViewModel.achievementList.observe(viewLifecycleOwner) { list ->
            // Count how many are unlocked
            val unlockedCount = list.count { it.isUnlocked }
            binding.tvAchievementsCount.text = unlockedCount.toString()
        }
    }

    private fun handleLogout() {
        val prefs = requireContext().getSharedPreferences("APP_PREFS", Context.MODE_PRIVATE)
        prefs.edit().remove("TOKEN").apply()
        findNavController().navigate(R.id.action_nav_home_to_loginFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}