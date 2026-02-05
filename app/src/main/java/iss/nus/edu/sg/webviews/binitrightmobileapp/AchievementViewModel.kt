package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.Achievement
import iss.nus.edu.sg.webviews.binitrightmobileapp.network.RetrofitClient
import kotlinx.coroutines.launch

class AchievementViewModel(application: Application) : AndroidViewModel(application) {

    private val _achievementList = MutableLiveData<List<Achievement>>()
    val achievementList: LiveData<List<Achievement>> = _achievementList

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val userId: Long by lazy {
        val prefs = getApplication<Application>().getSharedPreferences("APP_PREFS", Context.MODE_PRIVATE)
        prefs.getLong("USER_ID", -1L)
    }

    private val fixedAchievements = listOf(
        Achievement(1, "First Submission", "Submit your first recycling item.", "Recycle 1 item", "https://img.icons8.com/color/96/seed.png"),
        Achievement(2, "Recycling Master", "Complete 10 recycling submissions.", "Recycle 10 times", "https://img.icons8.com/color/96/recycle-sign.png"),
        Achievement(3, "Plastic Slayer", "Help keep 50 plastic bottles out of the ocean.", "Recycle 50 Plastic items", "https://img.icons8.com/color/96/plastic.png"),
        Achievement(4, "The 100 Club", "A true eco-warrior legend.", "Earn 100 points in total", "https://img.icons8.com/color/96/trophy.png"),
        Achievement(5, "The Collector", "Save up your rewards points.", "Hold 5000 points", "https://img.icons8.com/color/96/hamster.png"),
        Achievement(6, "Rising Star", "Advance your environmental impact rank.", "Reach Rank 2", "https://img.icons8.com/color/96/upgrade.png"),
        Achievement(7, "Early Bird", "Complete a check-in early in the morning.", "Check-in 06:00-08:00", "https://img.icons8.com/color/96/sun.png"),
        Achievement(8, "Night Owl", "Contribute to recycling late at night.", "Recycle after 22:00", "https://img.icons8.com/color/96/owl.png"),
        Achievement(9, "Eagle Eye", "Help maintain the community's bins.", "Report 1 Issue", "https://img.icons8.com/color/96/visible.png"),
        Achievement(10, "First Pot of Gold", "Redeem your points for a reward.", "Redeem 1 Reward", "https://img.icons8.com/color/96/coins.png")
    )

    init {
        fetchAchievements()
    }

    fun fetchAchievements() {
        if (userId == -1L) {
            _achievementList.value = fixedAchievements
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = RetrofitClient.instance.getAchievementsWithStatus(userId)
                if (response.isSuccessful && response.body() != null) {
                    val remoteData = response.body()!!
                    val remoteIds = remoteData.filter { it.isUnlocked }.map { it.id }.toSet()
                    
                    val mergedList = fixedAchievements.map { ach ->
                        if (remoteIds.contains(ach.id)) {
                            ach.copy(isUnlocked = true)
                        } else {
                            ach
                        }
                    }
                    _achievementList.value = mergedList
                } else {
                    _achievementList.value = fixedAchievements
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _achievementList.value = fixedAchievements
            } finally {
                _isLoading.value = false
            }
        }
    }
}
