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

    init {
        fetchAchievements()
    }

    fun fetchAchievements() {
        if (userId == -1L) {
            loadMockData()
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = RetrofitClient.instance.getAchievementsWithStatus(userId)
                if (response.isSuccessful && response.body() != null) {
                    _achievementList.value = response.body()
                } else {
                    loadMockData()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                loadMockData()
            } finally {
                _isLoading.value = false
            }
        }
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
                dateAchieved = null
            ),
            Achievement(
                id = 2,
                name = "Plastic Slayer",
                description = "You have kept 50 plastic bottles out of the ocean.",
                criteria = "Recycle 50 Plastic items",
                badgeIconUrl = "https://img.icons8.com/color/96/plastic.png",
                isUnlocked = true,
                dateAchieved = null
            )
        )
        _achievementList.value = mockData
    }
}