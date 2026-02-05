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
        // Navigation to Recycle History
        binding.recycleHistory.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_recycleHistory)
        }

        // Navigation to Avatar Customization
        binding.customizeAvatarBtn.setOnClickListener {
            // Updated to match the action ID in your nav_graph
            findNavController().navigate(R.id.action_profile_to_avatarCustomizationFragment)
        }

        binding.logoutBtn.setOnClickListener {
            handleLogout()
        }
    }

    private fun loadProfileData() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Call your new endpoint
                val response = RetrofitClient.apiService().getProfileSummary()

                if (response.isSuccessful) {
                    val profile = response.body()
                    profile?.let {
                        // Update UI with DTO data
                        binding.profileName.text = it.name
                        print(it.name)
                        binding.pointsDisplay.text = "${it.pointBalance} Points"

                        // Map the avatar name to local drawable
                        val drawableName = it.equippedAvatarName.lowercase().replace(" ", "_")
                        val resId = requireContext().resources.getIdentifier(
                            drawableName, "drawable", requireContext().packageName
                        )

                        if (resId != 0) {
                            binding.avatarImage.setImageResource(resId)
                        } else {
                            // Fallback if the PNG is missing
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
        // 1. Clear the token from SharedPreferences
        val prefs = requireContext().getSharedPreferences("APP_PREFS", Context.MODE_PRIVATE)
        prefs.edit().remove("TOKEN").apply()

        // 2. Navigate to Login and clear the navigation backstack
        // Ensure action_profile_to_loginFragment exists in your nav_graph.xml
        findNavController().navigate(R.id.action_profile_to_loginFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}