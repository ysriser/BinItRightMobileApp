package iss.nus.edu.sg.webviews.binitrightmobileapp

import iss.nus.edu.sg.webviews.binitrightmobileapp.model.RecycleHistoryModel
import iss.nus.edu.sg.webviews.binitrightmobileapp.network.RetrofitClient

class RecycleHistoryRepository {

    suspend fun getHistory(userId: Long): List<RecycleHistoryModel> {
        return RetrofitClient
            .apiService()
            .getRecycleHistory(userId)
    }
}
