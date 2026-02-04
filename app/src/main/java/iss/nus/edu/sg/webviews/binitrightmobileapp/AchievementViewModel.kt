package iss.nus.edu.sg.webviews.binitrightmobileapp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.Achievement

class AchievementViewModel : ViewModel() {

    private val _achievementList = MutableLiveData<List<Achievement>>()
    val achievementList: LiveData<List<Achievement>> = _achievementList

    init {
        loadMockData()
    }

    private fun loadMockData() {
        val mockData = listOf(
            Achievement(
                id = 1,
                name = "First Submission",
                description = "Submit your first recycling item to earn this badge.",
                criteria = "Recycle 1 item",
                badgeIconUrl = "https://img.icons8.com/color/96/seed.png",
                isUnlocked = true,
                dateAchieved = "2026-01-15"
            ),
            Achievement(
                id = 2,
                name = "Plastic Slayer",
                description = "You have kept 50 plastic bottles out of the ocean.",
                criteria = "Recycle 50 Plastic items",
                badgeIconUrl = "https://img.icons8.com/color/96/plastic.png",
                isUnlocked = true,
                dateAchieved = "2026-02-01"
            ),
            Achievement(
                id = 3,
                name = "E-Waste Expert",
                description = "Safely disposed of electronic components.",
                criteria = "Recycle 10 E-Waste items",
                badgeIconUrl = "https://img.icons8.com/color/96/electronics.png",
                isUnlocked = false,
                dateAchieved = null
            ),
            Achievement(
                id = 4,
                name = "The 100 Club",
                description = "A true eco-warrior legend.",
                criteria = "Earn 100 points in total",
                badgeIconUrl = "https://img.icons8.com/color/96/trophy.png",
                isUnlocked = false,
                dateAchieved = null
            )
        )
        _achievementList.value = mockData
    }
}