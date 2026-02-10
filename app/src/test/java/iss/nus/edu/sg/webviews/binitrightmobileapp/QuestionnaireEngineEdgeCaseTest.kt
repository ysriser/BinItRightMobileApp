package iss.nus.edu.sg.webviews.binitrightmobileapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class QuestionnaireEngineEdgeCaseTest {

    private fun buildEngineWithCycle(): QuestionnaireEngine {
        val config = QuestionnaireConfig(
            startQuestionId = "q1",
            questions = listOf(
                QuestionNode(
                    id = "q1",
                    question = "Q1",
                    subtitle = null,
                    options = listOf(OptionNode("o1", "to q2", "q2")),
                ),
                QuestionNode(
                    id = "q2",
                    question = "Q2",
                    subtitle = null,
                    options = listOf(OptionNode("o2", "back to q1", "q1")),
                ),
            ),
            outcomes = emptyList(),
        )
        return QuestionnaireEngine(RuntimeEnvironment.getApplication(), config)
    }

    @Test
    fun selectOption_withUnknownOption_returnsError() {
        val config = QuestionnaireConfig(
            startQuestionId = "q1",
            questions = listOf(
                QuestionNode(
                    id = "q1",
                    question = "Q1",
                    subtitle = null,
                    options = listOf(OptionNode("o1", "to outcome", "out1")),
                )
            ),
            outcomes = listOf(
                OutcomeNode("out1", "Paper", "Recycle", "HIGH", "ok", listOf("tip"))
            ),
        )
        val engine = QuestionnaireEngine(RuntimeEnvironment.getApplication(), config)

        val next = engine.selectOption("q1", "not-existing")

        assertEquals("error", next)
        assertTrue(engine.getAnswersSummary().isEmpty())
    }

    @Test
    fun getCurrentQuestion_addsBackOptionOnlyOnceAfterFirstStep() {
        val engine = buildEngineWithCycle()

        engine.selectOption("q1", "o1")

        val firstRead = engine.getCurrentQuestion() ?: error("Expected question")
        val secondRead = engine.getCurrentQuestion() ?: error("Expected question")

        assertEquals(1, firstRead.options.count { it.id == "BACK_ACTION" })
        assertEquals(1, secondRead.options.count { it.id == "BACK_ACTION" })
    }

    @Test
    fun getProgress_handlesCyclicQuestionFlowSafely() {
        val engine = buildEngineWithCycle()

        val startProgress = engine.getProgress()
        engine.selectOption("q1", "o1")
        val afterOneStep = engine.getProgress()

        assertTrue(startProgress.first >= 1)
        assertTrue(startProgress.second >= startProgress.first)
        assertTrue(afterOneStep.first >= 1)
        assertTrue(afterOneStep.second >= afterOneStep.first)
    }
}
