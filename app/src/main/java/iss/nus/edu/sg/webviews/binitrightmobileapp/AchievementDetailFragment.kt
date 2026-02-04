package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import coil.load
import iss.nus.edu.sg.webviews.binitrightmobileapp.databinding.FragmentAchievementDetailBinding
import kotlinx.coroutines.launch

class AchievementDetailFragment : Fragment() {

    private var _binding: FragmentAchievementDetailBinding? = null
    private val binding get() = _binding!!

    private val TAG = "DEBUG_USER"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAchievementDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val name = arguments?.getString("name") ?: "Achievement"
        val description = arguments?.getString("description") ?: ""
        val criteria = arguments?.getString("criteria") ?: ""
        val iconUrl = arguments?.getString("iconUrl") ?: ""
        val isUnlocked = arguments?.getBoolean("isUnlocked") ?: false
        val dateAchieved = arguments?.getString("dateAchieved")

        binding.tvDetailName.text = name
        binding.tvDetailDescription.text = description
        binding.tvDetailCriteria.text = criteria

        binding.tvDetailUserName.text = "Loading..."

        binding.ivDetailBadge.load(iconUrl) {
            crossfade(true)
            placeholder(android.R.drawable.ic_menu_help)
            error(android.R.drawable.ic_lock_idle_lock)
        }

        if (isUnlocked) {
            binding.tvUnlockStatus.text = "UNLOCKED"
            binding.tvUnlockStatus.setBackgroundColor(Color.parseColor("#00C853"))
            binding.ivDetailBadge.colorFilter = null
            binding.btnShare.isEnabled = true

            binding.lblDateAchieved.isVisible = true
            binding.tvDateAchieved.isVisible = true
            binding.tvDateAchieved.text = dateAchieved ?: "Unknown Date"

            fetchUserName(name, description)

        } else {
            binding.tvUnlockStatus.text = "LOCKED"
            binding.tvUnlockStatus.setBackgroundColor(Color.parseColor("#78909C"))

            val matrix = ColorMatrix()
            matrix.setSaturation(0f)
            binding.ivDetailBadge.colorFilter = ColorMatrixColorFilter(matrix)

            binding.btnShare.text = "Keep Recycling to Unlock"
            binding.btnShare.isEnabled = false
            binding.btnShare.setBackgroundColor(Color.LTGRAY)

            binding.lblDateAchieved.isVisible = false
            binding.tvDateAchieved.isVisible = false

            binding.tvDetailUserName.text = "-"
        }

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun fetchUserName(achievementName: String, achievementDesc: String) {
        val prefs = requireContext().getSharedPreferences("APP_PREFS", Context.MODE_PRIVATE)
        val token = prefs.getString("TOKEN", "") ?: ""

        Log.d(TAG, "Starting to fetch user profile. Token: $token")

        if (token.isEmpty()) {
            Log.e(TAG, "Error: Token is empty!")
            binding.tvDetailUserName.text = "Error: No Token"
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {

                val response = RetrofitClient.instance.getUserProfile("Bearer $token")

                Log.d(TAG, "Response Code: ${response.code()}")

                if (response.isSuccessful && response.body() != null) {
                    val user = response.body()
                    Log.d(TAG, "Fetch Success! Username: ${user?.username}")

                    val userName = user?.username ?: "Unknown User"
                    binding.tvDetailUserName.text = userName

                    binding.btnShare.setOnClickListener {
                        shareAchievement(achievementName, achievementDesc, userName)
                    }
                } else {
                    Log.e(TAG, "Fetch Failed. Error Body: ${response.errorBody()?.string()}")
                    binding.tvDetailUserName.text = "Unknown User (Error ${response.code()})"
                    binding.btnShare.setOnClickListener {
                        shareAchievement(achievementName, achievementDesc, "Me")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during fetch: ${e.message}")
                e.printStackTrace()
                binding.tvDetailUserName.text = "Network Error"
                binding.btnShare.setOnClickListener {
                    shareAchievement(achievementName, achievementDesc, "Me")
                }
            }
        }
    }

    private fun shareAchievement(title: String, desc: String, user: String) {
        val shareText = "🏆 $user just unlocked the '$title' achievement in BinItRight! \n\n$desc \n\nJoin us in recycling to save the planet! 🌍"

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "My Recycling Achievement")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        startActivity(Intent.createChooser(intent, "Share Achievement via"))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}