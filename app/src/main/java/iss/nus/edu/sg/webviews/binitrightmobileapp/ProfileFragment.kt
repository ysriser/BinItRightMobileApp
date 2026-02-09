package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import iss.nus.edu.sg.webviews.binitrightmobileapp.databinding.FragmentProfileBinding
import kotlinx.coroutines.launch
import iss.nus.edu.sg.webviews.binitrightmobileapp.network.RetrofitClient


class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Fetch data from API as soon as view is created
        loadProfileData()

        // Navigation to Leaderboard
        binding.leaderboardCard.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_leaderboard)
        }

        // Navigation to Reward Shop using SafeArgs
        binding.rewardShopCard.setOnClickListener {
            // Retrieve current points from the UI state
            val totalPoints = binding.gridPoints.text.toString().toIntOrNull() ?: 0

            val action = ProfileFragmentDirections.actionProfileToRewardShopFragment(totalPoints)
            findNavController().navigate(action)
        }

        // Navigation to Recycle History
        binding.recycleHistory.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_recycleHistory)
        }

        // Navigation to Avatar Customization
        binding.customizeAvatarBtn.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_avatarCustomizationFragment)
        }

        binding.logoutBtn.setOnClickListener {
            handleLogout()
        }
    }

    private fun loadProfileData() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Notice: using the apiService() function as defined in your RetrofitClient
                val response = RetrofitClient.apiService().getProfileSummary()

                if (response.isSuccessful) {
                    val profile = response.body()
                    profile?.let {
                        // 1. Update Header Info
                        binding.profileName.text = it.name
                        binding.pointsDisplay.text = "${it.pointBalance} Points"
                        binding.summaryRecycled.text = "${it.totalRecycled}"

                        // 2. Update Stats Grid
                        binding.gridPoints.text = it.pointBalance.toString()
                        binding.gridItems.text = it.totalRecycled.toString()

                        // 3. Update Avatar Image
                        val drawableName = it.equippedAvatarName.lowercase().replace(" ", "_")
                        val resId = requireContext().resources.getIdentifier(
                            drawableName, "drawable", requireContext().packageName
                        )

                        if (resId != 0) {
                            binding.avatarImage.setImageResource(resId)
                        } else {
                            binding.avatarImage.setImageResource(R.drawable.default_avatar)
                        }
                    }
                } else {
                    Log.e("ProfileFragment", "Server Error: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("ProfileFragment", "Network Failure: ${e.message}")
            }
        }
    }

    private fun handleLogout() {
        val prefs = requireContext().getSharedPreferences("APP_PREFS", Context.MODE_PRIVATE)
        prefs.edit().remove("TOKEN").apply()
        findNavController().navigate(R.id.action_profile_to_loginFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}