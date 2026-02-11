package iss.nus.edu.sg.webviews.binitrightmobileapp

import iss.nus.edu.sg.webviews.binitrightmobileapp.model.RecycleHistoryModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecycleHistoryRepositoryImplTest {

    @Test
    fun getHistory_returnsDataFromInjectedFetcher() = runTest {
        val expected = listOf(
            RecycleHistoryModel("Plastic", "plastic", "2026-01-01", 2),
            RecycleHistoryModel("Glass", "glass", "2026-01-02", 1)
        )

        val repository = RecycleHistoryRepositoryImpl(historyFetcher = { expected })

        val actual = repository.getHistory()
        assertEquals(expected, actual)
    }

    @Test
    fun getHistory_throwsWhenFetcherFails() = runTest {
        val repository = RecycleHistoryRepositoryImpl(historyFetcher = {
            throw IllegalStateException("history failed")
        })

        val error = runCatching { repository.getHistory() }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        assertEquals("history failed", error?.message)
    }
}
