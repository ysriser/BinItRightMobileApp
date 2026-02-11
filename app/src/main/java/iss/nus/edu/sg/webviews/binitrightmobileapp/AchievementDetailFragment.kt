package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import coil.load
import iss.nus.edu.sg.webviews.binitrightmobileapp.databinding.FragmentAchievementDetailBinding
import iss.nus.edu.sg.webviews.binitrightmobileapp.utils.JwtUtils

class AchievementDetailFragment : Fragment() {

    private var _binding: FragmentAchievementDetailBinding? = null
    private val binding get() = _binding!!

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

        binding.tvDetailName.text = name
        binding.tvDetailDescription.text = description
        binding.tvDetailCriteria.text = criteria

        binding.ivDetailBadge.load(iconUrl) {
            crossfade(true)
            placeholder(android.R.drawable.ic_menu_help)
            error(android.R.drawable.ic_lock_idle_lock)
        }

        val uiState = AchievementLogicUtils.getUIState(isUnlocked)

        binding.tvUnlockStatus.text = uiState.statusText
        binding.tvUnlockStatus.setBackgroundColor(Color.parseColor(uiState.statusColor))
        binding.btnShare.isEnabled = uiState.isShareEnabled

        if (!uiState.isShareEnabled) {
            binding.btnShare.text = "Keep Recycling to Unlock"
            binding.btnShare.setBackgroundColor(Color.LTGRAY)
            val matrix = ColorMatrix()
            matrix.setSaturation(0f)
            binding.ivDetailBadge.colorFilter = ColorMatrixColorFilter(matrix)
            binding.tvDetailUserName.text = "-"
        } else {
            binding.ivDetailBadge.colorFilter = null
            val prefs = requireContext().getSharedPreferences("APP_PREFS", Context.MODE_PRIVATE)
            val token = prefs.getString("JWT_TOKEN", "") ?: ""
            val userName = AchievementLogicUtils.getUsername(token)
            binding.tvDetailUserName.text = userName

            binding.btnShare.setOnClickListener {
                shareAchievement(name, description, userName)
            }
        }

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun shareAchievement(title: String, desc: String, user: String) {
        val shareText = AchievementLogicUtils.generateShareText(user, title, desc)
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

object AchievementLogicUtils {
    data class UIState(val statusText: String, val statusColor: String, val isShareEnabled: Boolean)

    fun getUIState(isUnlocked: Boolean): UIState {
        return if (isUnlocked) {
            UIState("UNLOCKED", "#00C853", true)
        } else {
            UIState("LOCKED", "#78909C", false)
        }
    }

    fun getUsername(token: String?): String {
        return if (!token.isNullOrEmpty()) {
            JwtUtils.getUsernameFromToken(token) ?: "Achiever"
        } else {
            "Achiever"
        }
    }

    fun generateShareText(user: String, title: String, desc: String): String {
        return "🏆 $user just unlocked the '$title' achievement in BinItRight! \n\n$desc \n\nJoin us in recycling to save the planet! 🌍"
    }
}