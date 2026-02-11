package iss.nus.edu.sg.webviews.binitrightmobileapp

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.RecycleHistoryModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecycleHistoryViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadHistory_updatesHistoryLiveData_withoutCoroutineTest() {
        val fakeData = listOf(
            RecycleHistoryModel("Plastic", "plastic", "2026-02-08", 2),
            RecycleHistoryModel("Glass", "glass", "2026-02-07", 1)
        )

        val vm = RecycleHistoryViewModel(FakeRepo(fakeData))

        vm.loadHistory()

        Thread.sleep(50)

        val history = vm.history.value
        assertEquals(2, history?.size)
        assertEquals("Plastic", history?.first()?.categoryName)
    }

    @Test
    fun loadHistory_failure_setsEmptyList() {
        val failingRepo = object : RecycleHistoryRepository {
            override suspend fun getHistory(): List<RecycleHistoryModel> {
                throw RuntimeException("API failed")
            }
        }

        val vm = RecycleHistoryViewModel(failingRepo)

        vm.loadHistory()

        // Allow coroutine to finish
        Thread.sleep(50)

        val history = vm.history.value

        assertNotNull(history)
        assertTrue(history!!.isEmpty())
    }

    private class FakeRepo(
        private val data: List<RecycleHistoryModel> = emptyList(),
        private val shouldFail: Boolean = false
    ) : RecycleHistoryRepository {

        override suspend fun getHistory(): List<RecycleHistoryModel> {
            if (shouldFail) throw RuntimeException("Fake failure")
            return data
        }
    }
}