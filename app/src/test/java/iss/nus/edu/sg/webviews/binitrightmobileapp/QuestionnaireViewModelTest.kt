package iss.nus.edu.sg.webviews.binitrightmobileapp

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.test.core.app.ApplicationProvider
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class QuestionnaireViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private fun buildViewModel(): QuestionnaireViewModel {
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

        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val engine = QuestionnaireEngine(app, config)
        return QuestionnaireViewModel(app, engine)
    }

    @Test
    fun init_loadsCurrentQuestionAndProgress() {
        val vm = buildViewModel()

        val question = vm.currentQuestion.getOrAwaitValue()
        val progress = vm.currentProgress.getOrAwaitValue()

        assertNotNull(question)
        assertTrue(progress.first >= 1)
        assertTrue(progress.second >= progress.first)
    }

    @Test
    fun selectOption_emitsNavigation_thenConsumeClears() {
        val vm = buildViewModel()
        val question = vm.currentQuestion.getOrAwaitValue() ?: error("Question must exist")
        assertTrue(question.options.isNotEmpty())

        val selected = question.options.first { it.id != "BACK_ACTION" }
        vm.selectOption(selected.id)

        val nav = vm.navigationEvent.getOrAwaitValueSkipNull()
        assertNotNull(nav)
        assertTrue(nav!!.isNotBlank())

        vm.consumeNavigation()
        assertNull(vm.navigationEvent.value)
    }

    @Test
    fun navigateBack_atStart_returnsFalse() {
        val vm = buildViewModel()

        val result = vm.navigateBack()

        assertFalse(result)
    }

    @Test
    fun selectOption_thenNavigateBack_returnsTrue() {
        val vm = buildViewModel()
        val question = vm.currentQuestion.getOrAwaitValue() ?: error("Question must exist")
        assertTrue(question.options.isNotEmpty())

        val selected = question.options.first { it.id != "BACK_ACTION" }
        vm.selectOption(selected.id)

        val backResult = vm.navigateBack()
        assertTrue(backResult)

        val summary = vm.answersSummary.getOrAwaitValue()
        assertTrue(summary.isEmpty())
    }
}

private fun <T> LiveData<T>.getOrAwaitValue(
    time: Long = 2,
    timeUnit: TimeUnit = TimeUnit.SECONDS,
): T {
    var data: T? = null
    val latch = CountDownLatch(1)
    val observer = object : Observer<T> {
        override fun onChanged(value: T) {
            data = value
            latch.countDown()
            this@getOrAwaitValue.removeObserver(this)
        }
    }
    this.observeForever(observer)
    if (!latch.await(time, timeUnit)) {
        this.removeObserver(observer)
        throw TimeoutException("LiveData value was never set.")
    }
    @Suppress("UNCHECKED_CAST")
    return data as T
}

private fun <T> LiveData<T>.getOrAwaitValueSkipNull(
    time: Long = 2,
    timeUnit: TimeUnit = TimeUnit.SECONDS,
): T? {
    var data: T? = null
    val latch = CountDownLatch(1)
    val observer = object : Observer<T> {
        override fun onChanged(value: T) {
            if (value == null) {
                return
            }
            data = value
            latch.countDown()
            this@getOrAwaitValueSkipNull.removeObserver(this)
        }
    }
    this.observeForever(observer)
    if (!latch.await(time, timeUnit)) {
        this.removeObserver(observer)
        throw TimeoutException("LiveData value was never set.")
    }
    return data
}
