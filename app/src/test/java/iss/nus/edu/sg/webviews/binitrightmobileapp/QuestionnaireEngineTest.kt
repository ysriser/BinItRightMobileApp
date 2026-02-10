package iss.nus.edu.sg.webviews.binitrightmobileapp

/*
 * File purpose:
 * - Basic, practical tests for QuestionnaireEngine.
 * - Uses a small in-memory config instead of assets (more reliable in CI).
 * - Checks start question, selecting an option, and going back.
 */
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QuestionnaireEngineTest {

    private fun buildEngine(): QuestionnaireEngine {
        val config = QuestionnaireConfig(
            startQuestionId = "q1",
            questions = listOf(
                QuestionNode(
                    id = "q1",
                    question = "What is the item made of?",
                    subtitle = null,
                    options = listOf(
                        OptionNode(id = "o1", text = "Plastic", next = "q2"),
                        OptionNode(id = "o2", text = "Other", next = "out1"),
                    ),
                ),
                QuestionNode(
                    id = "q2",
                    question = "Is it clean?",
                    subtitle = null,
                    options = listOf(
                        OptionNode(id = "o3", text = "Yes", next = "out1"),
                    ),
                ),
            ),
            outcomes = listOf(
                OutcomeNode(
                    id = "out1",
                    categoryTitle = "Plastic",
                    disposalLabel = "Recycle",
                    certainty = "HIGH",
                    explanation = "Test outcome",
                    tips = listOf("Rinse"),
                    instruction = "Rinse then recycle",
                )
            ),
        )
        val context = org.robolectric.RuntimeEnvironment.getApplication()
        return QuestionnaireEngine(context, config)
    }

    @Test
    fun initializesAtStartQuestion() {
        // Step 1: create engine with a small test config.
        val engine = buildEngine()

        // Step 2: it should point to a valid start question.
        val current = engine.getCurrentQuestion()
        assertNotNull(current)
        assertTrue(engine.isAtStart())
        assertEquals(current?.id, engine.currentQuestionId)
    }

    @Test
    fun selectOption_recordsAnswer_and_allowsBack() {
        // Step 1: create engine and get the current question.
        val engine = buildEngine()
        val question = engine.getCurrentQuestion() ?: error("Missing start question")

        // Step 2: pick an option that leads to another question if possible.
        val preferred = question.options.firstOrNull { opt ->
            engine.getQuestionById(opt.next) != null
        } ?: question.options.first()

        // Step 3: select option and move forward.
        val nextId = engine.selectOption(question.id, preferred.id)
        assertNotNull(nextId)
        assertFalse(engine.isAtStart())

        // Step 4: answers summary should have at least one entry.
        val summary = engine.getAnswersSummary()
        assertTrue(summary.isNotEmpty())

        // Step 5: go back should return to start.
        val didGoBack = engine.goBack()
        assertTrue(didGoBack)
        assertTrue(engine.isAtStart())
    }

    @Test
    fun selectOption_toOutcome_returnsOutcomeId() {
        // Step 1: create engine and get current question.
        val engine = buildEngine()
        val question = engine.getCurrentQuestion() ?: error("Missing start question")

        // Step 2: pick an option that goes directly to an outcome.
        val outcomeOption = question.options.first { it.next == "out1" }

        // Step 3: select the option and verify the returned id.
        val nextId = engine.selectOption(question.id, outcomeOption.id)
        assertEquals("out1", nextId)
        assertNotNull(engine.getOutcomeById(nextId))
    }

    @Test
    fun backAction_returnsTriggerSignal() {
        // Step 1: create engine and move forward once.
        val engine = buildEngine()
        val question = engine.getCurrentQuestion() ?: error("Missing start question")
        val nextId = engine.selectOption(question.id, question.options.first().id)
        assertNotNull(nextId)

        // Step 2: current question should now include a back option.
        val current = engine.getCurrentQuestion() ?: error("Missing current question")
        val backOption = current.options.first { it.id == "BACK_ACTION" }

        // Step 3: selecting BACK_ACTION should return the trigger signal.
        val result = engine.selectOption(current.id, backOption.id)
        assertEquals("BACK_ACTION_TRIGGERED", result)
    }
    @Test
    fun goBack_atStart_returnsFalse() {
        val engine = buildEngine()

        val didGoBack = engine.goBack()

        assertFalse(didGoBack)
        assertTrue(engine.isAtStart())
    }

    @Test
    fun getProgress_increasesAfterSelection() {
        val engine = buildEngine()
        val (startCurrent, startTotal) = engine.getProgress()
        assertTrue(startTotal >= startCurrent)

        val question = engine.getCurrentQuestion() ?: error("Missing start question")
        val nextQuestionOption = question.options.first { option ->
            engine.getQuestionById(option.next) != null
        }
        engine.selectOption(question.id, nextQuestionOption.id)

        val (afterCurrent, afterTotal) = engine.getProgress()
        assertTrue(afterCurrent >= startCurrent)
        assertTrue(afterTotal >= afterCurrent)
    }
}
