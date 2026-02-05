package iss.nus.edu.sg.webviews.binitrightmobileapp

/**
 * Needs to be modify to fit our backend and UI requirements in the future
 */
data class ScanResult(
    val category: String,
    val recyclable: Boolean, // Critical
    val confidence: Float, // Critical
    val instruction: String? = null, // Critical
    val instructions: List<String> = emptyList(), // Recommended
    val disposalMethod: String? = null, // Optional
    val categoryId: String? = null, // Recommended
    val binType: String? = null, // Optional
    val rationaleTags: List<String> = emptyList() // Optional
) : java.io.Serializable

data class Tier1Result(
    val category: String,
    val confidence: Float,
    val top3: List<Map<String, Any>>, // keeping it flexible for JSON serialization or use specific data class
    val escalate: Boolean
)

data class FeedbackRequest(
    val imageId: String,
    val userFeedback: Boolean, // true = accurate, false = incorrect
    val timestamp: Long
)
