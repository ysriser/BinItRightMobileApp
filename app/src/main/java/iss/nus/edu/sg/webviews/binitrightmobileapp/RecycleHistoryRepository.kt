package iss.nus.edu.sg.webviews.binitrightmobileapp

import iss.nus.edu.sg.webviews.binitrightmobileapp.model.RecycleHistoryModel
import iss.nus.edu.sg.webviews.binitrightmobileapp.network.RetrofitClient

class RecycleHistoryRepository {
    suspend fun getRecycleHistory(): List<RecycleHistoryModel> {
        val response = RetrofitClient.apiService().getRecycleHistory()
        return if (response.isSuccessful) {
            response.body() ?: emptyList()
        } else {
            emptyList()
        }
    }
}