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
import iss.nus.edu.sg.webviews.binitrightmobileapp.utils.JwtUtils
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

        binding.tvDetailName.text = name
        binding.tvDetailDescription.text = description
        binding.tvDetailCriteria.text = criteria

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

            val prefs = requireContext().getSharedPreferences("APP_PREFS", Context.MODE_PRIVATE)
            val token = prefs.getString("JWT_TOKEN", "") ?: ""
            val userName = if (token.isNotEmpty()) {
                JwtUtils.getUsernameFromToken(token) ?: "Achiever"
            } else {
                "Achiever"
            }
            binding.tvDetailUserName.text = userName

            binding.btnShare.setOnClickListener {
                shareAchievement(name, description, userName)
            }

        } else {
            binding.tvUnlockStatus.text = "LOCKED"
            binding.tvUnlockStatus.setBackgroundColor(Color.parseColor("#78909C"))

            val matrix = ColorMatrix()
            matrix.setSaturation(0f)
            binding.ivDetailBadge.colorFilter = ColorMatrixColorFilter(matrix)

            binding.btnShare.text = "Keep Recycling to Unlock"
            binding.btnShare.isEnabled = false
            binding.btnShare.setBackgroundColor(Color.LTGRAY)

            binding.tvDetailUserName.text = "-"
        }

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
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
