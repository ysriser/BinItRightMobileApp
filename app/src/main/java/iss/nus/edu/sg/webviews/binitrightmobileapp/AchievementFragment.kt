package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import iss.nus.edu.sg.webviews.binitrightmobileapp.databinding.FragmentAchievementsBinding

class AchievementsFragment : Fragment() {

    private var _binding: FragmentAchievementsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AchievementViewModel by viewModels()
    private lateinit var adapter: AchievementAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAchievementsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = AchievementAdapter { clickedItem ->
            val bundle = Bundle().apply {
                putString("name", clickedItem.name)
                putString("description", clickedItem.description)
                putString("criteria", clickedItem.criteria)
                putString("iconUrl", clickedItem.badgeIconUrl)
                putBoolean("isUnlocked", clickedItem.isUnlocked)
                putString("dateAchieved", clickedItem.dateAchieved)
            }
            findNavController().navigate(R.id.action_achievementsFragment_to_achievementDetailFragment, bundle)
        }

        binding.rvAchievements.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@AchievementsFragment.adapter
        }

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        viewModel.achievementList.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            updateProgressCard(list)
        }
    }

    private fun updateProgressCard(list: List<iss.nus.edu.sg.webviews.binitrightmobileapp.model.Achievement>) {
        val total = list.size
        val unlocked = list.count { it.isUnlocked }
        val remaining = total - unlocked

        binding.tvProgressFraction.text = "$unlocked/$total"

        binding.progressBarOverall.max = total
        binding.progressBarOverall.progress = unlocked

        if (remaining > 0) {
            binding.tvProgressMessage.text = "$remaining more to unlock!"
        } else {
            binding.tvProgressMessage.text = "All achievements completed! 🎉"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}