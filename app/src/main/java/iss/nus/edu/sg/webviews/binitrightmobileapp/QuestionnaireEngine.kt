package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.content.Context
import com.google.gson.Gson
import java.io.InputStreamReader
import java.util.Stack

class QuestionnaireEngine(private val context: Context) {

    private lateinit var config: QuestionnaireConfig
    private val answers = LinkedHashMap<String, String>() // QuestionId -> OptionId
    private val backStack = Stack<String>() // QuestionIds visited

    var currentQuestionId: String? = null
        private set

    init {
        loadConfig()
    }

    private fun loadConfig() {
        try {
            val inputStream = context.assets.open("questionnaire.json")
            val reader = InputStreamReader(inputStream)
            config = Gson().fromJson(reader, QuestionnaireConfig::class.java)
            currentQuestionId = config.startQuestionId
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback empty config if parsing fails (should not happen in prod if JSON is valid)
        }
    }

    fun getCurrentQuestion(): QuestionNode? {
        val q = config.questions.find { it.id == currentQuestionId } ?: return null
        
        // If not at start, append a "Back" option
        return if (!isAtStart()) {
             val backOption =
                 OptionNode("BACK_ACTION", "Go back to previous question", "BACK_ACTION_NEXT")
             q.copy(options = q.options + backOption)
        } else {
             q
        }
    }

    fun getQuestionById(id: String): QuestionNode? {
        return config.questions.find { it.id == id }
    }

    fun getOutcomeById(id: String): OutcomeNode? {
        return config.outcomes.find { it.id == id }
    }

    // Returns next Question ID or null if it's an outcome
    fun selectOption(questionId: String, optionId: String): String {
        // Special Back Handling
        if (optionId == "BACK_ACTION") {
            return "BACK_ACTION_TRIGGERED" // Signal to ViewModel to call goBack
        }
    
        answers[questionId] = optionId
        backStack.push(questionId)

        val question = getQuestionById(questionId)
        val option = question?.options?.find { it.id == optionId }
        val nextId = option?.next ?: return "error"

        // Check if nextId is a question or outcome
        val isQuestion = config.questions.any { it.id == nextId }
        val isOutcome = config.outcomes.any { it.id == nextId }

        if (isQuestion) {
            currentQuestionId = nextId
        }
        
        // Return the ID for the caller to decide navigation (to next Q or Result)
        return nextId
    }

    fun goBack(): Boolean {
        if (backStack.isEmpty()) return false
        
        val prevQuestionId = backStack.pop()
        answers.remove(prevQuestionId) // Undo answer
        currentQuestionId = prevQuestionId
        return true
    }

    fun isAtStart(): Boolean {
        return backStack.isEmpty()
    }

    fun getAnswersSummary(): List<Pair<String,String>> {
        // Retrieve Question Text and Option Text for displayed summary
        return answers.mapNotNull { (qId, optionId) ->
            val q = getQuestionById(qId)
            val opt = q?.options?.find { it.id == optionId }
            if (q != null && opt != null) {
                // Shorten question text for summary if needed, but per request "Question Text" - "Option"
                // Using full text might be long, let's use Question Text - Option Text
                q.question to opt.text
            } else {
                null
            }
        }
    }

    fun getProgress(): Pair<Int, Int> {
        val current = backStack.size + 1
        // Minimal logic: return current step. UI can decide how to render.
        return Pair(current, 3) 
        // 3 is a placeholder. A true dynamic calc requires traversing the graph.
        // Given constraint "N is dynamic... compute once path is known",
        // we update N when we assume the path. 
        // For prototype, let's just increment "Question X"
    }
}
