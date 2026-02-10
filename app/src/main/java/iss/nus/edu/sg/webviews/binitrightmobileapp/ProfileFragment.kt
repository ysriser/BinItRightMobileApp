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

        loadProfileData()

        binding.leaderboardCard.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_leaderboard)
        }

        binding.rewardShopCard.setOnClickListener {
            val totalPoints = binding.gridPoints.text.toString().toIntOrNull() ?: 0
            val action = ProfileFragmentDirections.actionProfileToRewardShopFragment(totalPoints)
            findNavController().navigate(action)
        }

        binding.recycleHistory.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_recycleHistory)
        }

        binding.customizeAvatarBtn.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_avatarCustomizationFragment)
        }

        binding.achievementsCard.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_achievements)
        }

        binding.logoutBtn.setOnClickListener {
            handleLogout()
        }
    }

    private fun loadProfileData() {
        val userId = requireActivity()
            .getSharedPreferences("APP_PREFS", Context.MODE_PRIVATE)
            .getLong("USER_ID", -1L)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val api = RetrofitClient.apiService()
                val response = api.getProfileSummary()

                if (response.isSuccessful) {
                    val profile = response.body()
                    profile?.let {
                        binding.profileName.text = it.name
                        binding.pointsDisplay.text = "${it.pointBalance} Points"
                        binding.summaryRecycled.text = "${it.totalRecycled}"

                        binding.gridPoints.text = it.pointBalance.toString()
                        binding.gridItems.text = it.totalRecycled.toString()

                        if (userId != -1L) {
                            val achievementsRes = api.getAchievementsWithStatus(userId)
                            if (achievementsRes.isSuccessful) {
                                val remoteData = achievementsRes.body() ?: emptyList()
                                val unlockedCount = remoteData.count { it.isUnlocked }
                                binding.gridAwards.text = unlockedCount.toString()
                                binding.summaryBadges.text = unlockedCount.toString()
                            }

                            val leaderboardRes = api.getLeaderboard()
                            if (leaderboardRes.isSuccessful) {
                                val leaderboardData = leaderboardRes.body() ?: emptyList()
                                val myIndex = leaderboardData.indexOfFirst { entry ->
                                    entry.userId == userId
                                }

                                if (myIndex != -1) {
                                    binding.gridRank.text = "#${myIndex + 1}"
                                } else {
                                    binding.gridRank.text = "N/A"
                                }
                            }
                        }

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