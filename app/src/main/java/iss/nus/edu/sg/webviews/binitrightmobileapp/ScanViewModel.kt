package iss.nus.edu.sg.webviews.binitrightmobileapp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.io.File

class ScanViewModel(private val repository: ScanRepository) : ViewModel() {

    private val _scanResult = MutableLiveData<Result<ScanResult>?>()
    val scanResult: LiveData<Result<ScanResult>?> = _scanResult

    private val _feedbackStatus = MutableLiveData<Result<Boolean>>()
    val feedbackStatus: LiveData<Result<Boolean>> = _feedbackStatus

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    fun scanImage(imageFile: File) {
        _isLoading.value = true
        _scanResult.value = null // Clear previous result
        viewModelScope.launch {
            _scanResult.value = repository.scanImage(imageFile)
            _isLoading.value = false
        }
    }
    
    fun resetScanState() {
        _scanResult.value = null
    }
    


    fun submitFeedback(feedback: FeedbackRequest) {
         viewModelScope.launch {
            _feedbackStatus.value = repository.sendFeedback(feedback)
        }
    }
}

class ScanViewModelFactory(private val context: android.content.Context) : ViewModelProvider.Factory {
    // Simple Config: CHANGE THIS TO SWITCH MODES
    // Options: "FAKE", "REAL", "LOCAL"
    private val DATA_SOURCE = "LOCAL" 

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScanViewModel::class.java)) {
            val repo: ScanRepository = when (DATA_SOURCE) {
                "REAL" -> RealScanRepository()
                "LOCAL" -> LocalModelScanRepository(context)
                else -> FakeScanRepository()
            }
            @Suppress("UNCHECKED_CAST")
            return ScanViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
