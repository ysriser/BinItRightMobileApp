package iss.nus.edu.sg.webviews.binitrightmobileapp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.NewsItem

class NewsViewModel : ViewModel() {

    private val _newsList = MutableLiveData<List<NewsItem>>()
    val newsList: LiveData<List<NewsItem>> = _newsList

    init {
        loadMockData()
    }

    private fun loadMockData() {
        val mockData = listOf(
            NewsItem(
                id = 1,
                title = "Why Plastic Recycling Matters",
                description = "Learn how you can save the ocean by recycling properly.",
                imageUrl = null,
                status = "Upcoming"
            ),
            NewsItem(
                id = 2,
                title = "E-Waste Hazards",
                description = "Electronic waste contains toxic components that harm the soil.",
                imageUrl = null,
                status = "Completed"
            )
        )
        _newsList.value = mockData
    }
}