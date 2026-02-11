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
import retrofit2.Response

class AchievementViewModel(
    application: Application,
    private val userIdProvider: () -> Long = {
        val prefs = application.getSharedPreferences("APP_PREFS", Context.MODE_PRIVATE)
        prefs.getLong("USER_ID", -1L)
    },
    private val remoteFetcher: suspend (Long) -> Response<List<Achievement>> = { userId ->
        RetrofitClient.apiService().getAchievementsWithStatus(userId)
    }
) : AndroidViewModel(application) {

    private val _achievementList = MutableLiveData<List<Achievement>>()
    val achievementList: LiveData<List<Achievement>> = _achievementList

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val fixedAchievements = listOf(
        Achievement(1L, "First Submission", "Submit your first recycling item.", "Recycle 1 item", "https://img.icons8.com/color/96/seed.png"),
        Achievement(2L, "Recycling Master", "Complete 10 recycling submissions.", "Recycle 10 times", "https://img.icons8.com/color/96/recycle-sign.png"),
        Achievement(3L, "Eco Enthusiast", "Maintain your dedication with 50 submissions.", "Recycle 50 times", "https://img.icons8.com/color/96/medal.png"),
        Achievement(4L, "Green Legend", "A monumental 100 recycling submissions!", "Recycle 100 times", "https://img.icons8.com/color/96/trophy.png"),
        Achievement(5L, "The Collector", "Save up your rewards points.", "Hold 5000 points", "https://img.icons8.com/color/96/hamster.png"),
        Achievement(6L, "Rising Star", "Advance your environmental impact rank.", "Reach Rank 2", "https://img.icons8.com/color/96/upgrade.png"),
        Achievement(7L, "Early Bird", "Complete a check-in early in the morning.", "Check-in 06:00-08:00", "https://img.icons8.com/color/96/sun.png"),
        Achievement(8L, "Night Owl", "Contribute to recycling late at night.", "Recycle after 22:00", "https://img.icons8.com/color/96/owl.png"),
        Achievement(9L, "Eagle Eye", "Help maintain the community's bins.", "Report 1 Issue", "https://img.icons8.com/color/96/visible.png"),
        Achievement(10L, "First Pot of Gold", "Redeem your points for a reward.", "Redeem 1 Reward", "https://img.icons8.com/color/96/coins.png")
    )

    init {
        fetchAchievements()
    }

    fun fetchAchievements() {
        val userId = userIdProvider()
        if (userId == -1L) {
            _achievementList.value = fixedAchievements
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = remoteFetcher(userId)

                if (response.isSuccessful && response.body() != null) {
                    val remoteData = response.body()!!

                    val unlockedIds = remoteData
                        .filter { it.isUnlocked }
                        .map { it.id }
                        .toSet()

                    val mergedList = fixedAchievements.map { ach ->
                        if (unlockedIds.contains(ach.id)) {
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
                _achievementList.value = fixedAchievements
            } finally {
                _isLoading.value = false
            }
        }
    }
}
