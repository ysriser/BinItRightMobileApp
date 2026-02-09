package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.content.Context
import com.google.gson.Gson
import java.io.InputStreamReader
import java.util.Stack

class QuestionnaireEngine(
    private val context: Context,
    private val configOverride: QuestionnaireConfig? = null,
) {

    private lateinit var config: QuestionnaireConfig
    private val answers = LinkedHashMap<String, String>()
    private val backStack = Stack<String>()

    var currentQuestionId: String? = null
        private set

    init {
        if (configOverride != null) {
            config = configOverride
            currentQuestionId = config.startQuestionId
        } else {
            loadConfig()
        }
    }

    private fun loadConfig() {
        try {
            val inputStream = context.assets.open("questionnaire.json")
            val reader = InputStreamReader(inputStream)
            config = Gson().fromJson(reader, QuestionnaireConfig::class.java)
            currentQuestionId = config.startQuestionId
        } catch (e: Exception) {
            e.printStackTrace()
            config = QuestionnaireConfig(startQuestionId = "", questions = emptyList(), outcomes = emptyList())
            currentQuestionId = null
        }
    }

    fun getCurrentQuestion(): QuestionNode? {
        val question = config.questions.find { it.id == currentQuestionId } ?: return null
        if (isAtStart()) {
            return question
        }

        val hasBackOption = question.options.any { it.id == BACK_OPTION_ID }
        return if (hasBackOption) {
            question
        } else {
            val backOption = OptionNode(BACK_OPTION_ID, "Go back to previous question", BACK_NAV_ID)
            question.copy(options = question.options + backOption)
        }
    }

    fun getQuestionById(id: String): QuestionNode? {
        return config.questions.find { it.id == id }
    }

    fun getOutcomeById(id: String): OutcomeNode? {
        return config.outcomes.find { it.id == id }
    }

    fun selectOption(questionId: String, optionId: String): String {
        if (optionId == BACK_OPTION_ID) {
            return BACK_TRIGGER_SIGNAL
        }

        answers[questionId] = optionId
        backStack.push(questionId)

        val question = getQuestionById(questionId)
        val option = question?.options?.find { it.id == optionId }
        val nextId = option?.next ?: return "error"

        if (config.questions.any { it.id == nextId }) {
            currentQuestionId = nextId
        }

        return nextId
    }

    fun goBack(): Boolean {
        if (backStack.isEmpty()) {
            return false
        }

        val previousQuestionId = backStack.pop()
        answers.remove(previousQuestionId)
        currentQuestionId = previousQuestionId
        return true
    }

    fun isAtStart(): Boolean {
        return backStack.isEmpty()
    }

    fun getAnswersSummary(): List<Pair<String, String>> {
        return answers.mapNotNull { (qId, optionId) ->
            val question = getQuestionById(qId)
            val selected = question?.options?.find { it.id == optionId }
            if (question != null && selected != null) {
                question.question to selected.text
            } else {
                null
            }
        }
    }

    fun getProgress(): Pair<Int, Int> {
        val current = (backStack.size + 1).coerceAtLeast(1)
        val remaining = estimateRemainingQuestions(currentQuestionId, mutableSetOf())
        val total = (current + remaining).coerceAtLeast(current)
        return current to total
    }

    private fun estimateRemainingQuestions(questionId: String?, visited: MutableSet<String>): Int {
        if (questionId.isNullOrBlank() || !visited.add(questionId)) {
            return 0
        }

        val question = getQuestionById(questionId) ?: return 0
        var maxRemaining = 0

        for (option in question.options) {
            if (option.id == BACK_OPTION_ID) {
                continue
            }

            val nextQuestion = getQuestionById(option.next)
            val remainingFromNext = if (nextQuestion == null) {
                0
            } else {
                1 + estimateRemainingQuestions(nextQuestion.id, visited.toMutableSet())
            }
            maxRemaining = maxOf(maxRemaining, remainingFromNext)
        }

        return maxRemaining
    }

    companion object {
        private const val BACK_OPTION_ID = "BACK_ACTION"
        private const val BACK_NAV_ID = "BACK_ACTION_NEXT"
        private const val BACK_TRIGGER_SIGNAL = "BACK_ACTION_TRIGGERED"
    }
}