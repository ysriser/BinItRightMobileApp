package iss.nus.edu.sg.webviews.binitrightmobileapp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.RecycleHistoryModel
import kotlinx.coroutines.launch

class RecycleHistoryViewModel(private val repo: RecycleHistoryRepository = RecycleHistoryRepositoryImpl()) : ViewModel() {

    private val _history = MutableLiveData<List<RecycleHistoryModel>>()
    val history: LiveData<List<RecycleHistoryModel>> = _history

    fun loadHistory() {
        viewModelScope.launch {
            try {
                _history.value = repo.getHistory()
            } catch (e: Exception) {
                _history.value = emptyList()
            }
        }
    }
}