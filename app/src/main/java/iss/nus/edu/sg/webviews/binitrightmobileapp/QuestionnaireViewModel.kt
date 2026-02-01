package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class QuestionnaireViewModel(application: Application) : AndroidViewModel(application) {

    private val engine = QuestionnaireEngine(application)

    private val _currentQuestion = MutableLiveData<QuestionNode?>()
    val currentQuestion: LiveData<QuestionNode?> = _currentQuestion

    private val _currentProgress = MutableLiveData<Pair<Int, Int>>()
    val currentProgress: LiveData<Pair<Int, Int>> = _currentProgress

    private val _answersSummary = MutableLiveData<List<Pair<String, String>>>()
    val answersSummary: LiveData<List<Pair<String, String>>> = _answersSummary

    private val _navigationEvent = MutableLiveData<String?>() // Next ID (Question or Outcome)
    val navigationEvent: LiveData<String?> = _navigationEvent

    init {
        loadCurrentState()
    }

    private fun loadCurrentState() {
        _currentQuestion.value = engine.getCurrentQuestion()
        _currentProgress.value = engine.getProgress()
        _answersSummary.value = engine.getAnswersSummary()
    }

    fun selectOption(optionId: String) {
        val qId = engine.currentQuestionId ?: return
        val nextId = engine.selectOption(qId, optionId)
        
        if (nextId == "BACK_ACTION_TRIGGERED") {
            navigateBack()
            return
        }
        
        // If next is different, it means we moved forward
        _navigationEvent.value = nextId

        // If it's a question, update state immediately (for UI refresh)
        if (engine.getQuestionById(nextId) != null) {
            loadCurrentState()
        }
    }

    fun navigateBack(): Boolean {
        if (engine.goBack()) {
            loadCurrentState()
            return true
        }
        return false
    }
    
    fun getOutcome(id: String): OutcomeNode? {
        return engine.getOutcomeById(id)
    }

    fun consumeNavigation() {
        _navigationEvent.value = null
    }
}
