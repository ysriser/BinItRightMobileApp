package iss.nus.edu.sg.webviews.binitrightmobileapp

/**
 * Needs to be modify to fit our backend and UI requirements in the future
 */
data class ScanResult(
    val category: String,
    val recyclable: Boolean,
    val confidence: Float,
    val instructions: List<String> = emptyList(),
    val instruction: String? = null
)

data class FeedbackRequest(
    val imageId: String,
    val userFeedback: Boolean, // true = accurate, false = incorrect
    val timestamp: Long
)
