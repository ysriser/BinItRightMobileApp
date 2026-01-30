package iss.nus.edu.sg.webviews.binitrightmobileapp

/*
 * File purpose:
 * - Basic, practical tests for QuestionnaireEngine.
 * - Uses the real questionnaire.json asset (Robolectric provides assets).
 * - Checks start question, selecting an option, and going back.
 */

import android.content.Context
import androidx.test.core.app.ApplicationProvider
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
        val context = ApplicationProvider.getApplicationContext<Context>()
        return QuestionnaireEngine(context)
    }

    @Test
    fun initializesAtStartQuestion() {
        // Step 1: create engine (loads questionnaire.json).
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
}
