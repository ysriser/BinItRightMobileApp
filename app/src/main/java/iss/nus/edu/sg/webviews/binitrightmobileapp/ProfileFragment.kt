package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import iss.nus.edu.sg.webviews.binitrightmobileapp.databinding.FragmentProfileBinding
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.UserProfile
import iss.nus.edu.sg.webviews.binitrightmobileapp.network.RetrofitClient
import kotlinx.coroutines.launch
import java.util.Locale


class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val TAG = "ProfileFragment"
        private const val PREFS_NAME = "APP_PREFS"
        private const val PREF_USER_ID = "USER_ID"
        private const val PREF_TOKEN = "TOKEN"
        private const val INVALID_USER_ID = -1L
        private const val RANK_NOT_AVAILABLE = "N/A"
    }

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
        val userId = getStoredUserId()

        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                loadAndBindProfile(userId)
            }.onFailure { error ->
                Log.e(TAG, "Network Failure: ${error.message}", error)
            }
        }
    }

    private fun getStoredUserId(): Long {
        return requireActivity()
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(PREF_USER_ID, INVALID_USER_ID)
    }

    private suspend fun loadAndBindProfile(userId: Long) {
        val api = RetrofitClient.apiService()
        val response = api.getProfileSummary()

        if (!response.isSuccessful) {
            Log.e(TAG, "Server Error: ${response.code()}")
            return
        }

        val profile = response.body() ?: return
        bindProfileSummary(profile)
        bindAvatar(profile.equippedAvatarName)

        if (userId == INVALID_USER_ID) {
            return
        }
        bindAchievementSummary(api, userId)
        bindLeaderboardRank(api, userId)
    }

    private fun bindProfileSummary(profile: UserProfile) {
        binding.profileName.text = profile.name
        binding.pointsDisplay.text = getString(R.string.number_plain_int, profile.pointBalance)
        binding.summaryRecycled.text = getString(R.string.number_plain_int, profile.totalRecycled)
        binding.gridPoints.text = getString(R.string.number_plain_int, profile.pointBalance)
        binding.gridItems.text = getString(R.string.number_plain_int, profile.totalRecycled)
    }

    private suspend fun bindAchievementSummary(api: ApiService, userId: Long) {
        val achievementsRes = api.getAchievementsWithStatus(userId)
        if (!achievementsRes.isSuccessful) {
            return
        }

        val remoteData = achievementsRes.body() ?: emptyList()
        val unlockedCount = remoteData.count { it.isUnlocked }
        binding.gridAwards.text = getString(R.string.number_plain_int, unlockedCount)
        binding.summaryBadges.text = getString(R.string.number_plain_int, unlockedCount)
    }

    private suspend fun bindLeaderboardRank(api: ApiService, userId: Long) {
        val leaderboardRes = api.getLeaderboard()
        if (!leaderboardRes.isSuccessful) {
            return
        }

        val leaderboardData = leaderboardRes.body() ?: emptyList()
        val myIndex = leaderboardData.indexOfFirst { entry -> entry.userId == userId }
        binding.gridRank.text = if (myIndex != -1) {
            String.format(Locale.getDefault(), "#%d", myIndex + 1)
        } else {
            RANK_NOT_AVAILABLE
        }
    }

    private fun bindAvatar(equippedAvatarName: String) {
        binding.avatarImage.setImageResource(AvatarAssetResolver.drawableForName(equippedAvatarName))
    }

    private fun handleLogout() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { remove(PREF_TOKEN) }
        findNavController().navigate(R.id.action_profile_to_loginFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
