package iss.nus.edu.sg.webviews.binitrightmobileapp

import iss.nus.edu.sg.webviews.binitrightmobileapp.model.RecycleHistoryModel
import iss.nus.edu.sg.webviews.binitrightmobileapp.network.RetrofitClient

fun interface RecycleHistoryRepository {
    suspend fun getHistory(): List<RecycleHistoryModel>
}

class RecycleHistoryRepositoryImpl(
    private val historyFetcher: suspend () -> List<RecycleHistoryModel> = {
        RetrofitClient.apiService().getRecycleHistory()
    }
) : RecycleHistoryRepository {
    override suspend fun getHistory(): List<RecycleHistoryModel> {
        return historyFetcher()
    }
}
