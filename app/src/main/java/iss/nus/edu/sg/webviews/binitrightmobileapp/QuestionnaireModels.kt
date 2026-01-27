package iss.nus.edu.sg.webviews.binitrightmobileapp

import java.io.Serializable

enum class Certainty { HIGH, MEDIUM, LOW }

data class GuidanceResult(
    val categoryTitle: String,
    val disposalLabel: String,
    val certainty: Certainty,
    val explanation: String,
    val tips: List<String>
) : Serializable

data class QuestionnaireConfig(
    val startQuestionId: String,
    val questions: List<QuestionNode>,
    val outcomes: List<OutcomeNode>
)

data class QuestionNode(
    val id: String,
    val title: String? = "Item Questionnaire",
    val question: String,
    val subtitle: String?,
    val options: List<OptionNode>
)

data class OptionNode(
    val id: String,
    val text: String,
    val next: String
)

data class OutcomeNode(
    val id: String,
    val categoryTitle: String,
    val disposalLabel: String,
    val certainty: String,
    val explanation: String,
    val tips: List<String>
)

// Helper to bundle result for passing between fragments
data class SerializableOutcome(
    val categoryTitle: String,
    val disposalLabel: String,
    val certainty: String,
    val explanation: String,
    val tips: List<String>
) : Serializable
