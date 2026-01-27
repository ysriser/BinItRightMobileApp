package iss.nus.edu.sg.webviews.binitrightmobileapp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.io.File

class ScanViewModel(private val repository: ScanRepository) : ViewModel() {

    private val _scanResult = MutableLiveData<Result<ScanResult>>()
    val scanResult: LiveData<Result<ScanResult>> = _scanResult

    private val _feedbackStatus = MutableLiveData<Result<Boolean>>()
    val feedbackStatus: LiveData<Result<Boolean>> = _feedbackStatus

    fun scanImage(imageFile: File) {
        viewModelScope.launch {
            _scanResult.value = repository.scanImage(imageFile)
        }
    }

    fun submitFeedback(feedback: FeedbackRequest) {
         viewModelScope.launch {
            _feedbackStatus.value = repository.sendFeedback(feedback)
        }
    }
}

class ScanViewModelFactory(private val repository: ScanRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScanViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ScanViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
