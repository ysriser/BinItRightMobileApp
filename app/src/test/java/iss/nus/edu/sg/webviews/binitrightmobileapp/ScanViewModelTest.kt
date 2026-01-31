package iss.nus.edu.sg.webviews.binitrightmobileapp

/*
 * File purpose:
 * - Simple unit tests for ScanViewModel.
 * - Uses a fake repository so tests are deterministic and fast.
 * - Focuses on practical behavior: scan result is set and reset works.
 */

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.Before

@OptIn(ExperimentalCoroutinesApi::class)
class ScanViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        // Use a test dispatcher for viewModelScope coroutines.
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    private class FakeRepo : ScanRepository {
        override suspend fun scanImage(imageFile: File): Result<ScanResult> {
            return Result.success(
                ScanResult(
                    category = "Glass",
                    recyclable = true,
                    confidence = 0.9f,
                    instructions = listOf("Rinse"),
                    instruction = "1. Rinse",
                )
            )
        }

        override suspend fun sendFeedback(feedback: FeedbackRequest): Result<Boolean> {
            return Result.success(true)
        }
    }

    @Test
    fun scanImage_setsResult() = runTest {
        // Step 1: create ViewModel with a fake repository.
        val vm = ScanViewModel(FakeRepo())

        // Step 2: initial state should not be loading.
        assertFalse(vm.isLoading.getOrAwaitValue())

        // Step 3: run scan.
        vm.scanImage(File("dummy.jpg"))

        // Step 4: result should be available and successful.
        val result = vm.scanResult.getOrAwaitValueSkipNull()
        assertNotNull(result)
        assertTrue(result!!.isSuccess)
        assertEquals("Glass", result.getOrNull()?.category)

        // Step 5: loading should be false after completion.
        assertFalse(vm.isLoading.getOrAwaitValue())
    }

    @Test
    fun resetScanState_clearsResult() = runTest {
        // Step 1: create ViewModel with a fake repository.
        val vm = ScanViewModel(FakeRepo())

        // Step 2: run scan so a result exists.
        vm.scanImage(File("dummy.jpg"))
        assertNotNull(vm.scanResult.getOrAwaitValueSkipNull())

        // Step 3: reset should clear any previous scan result.
        vm.resetScanState()
        assertNull(vm.scanResult.getOrAwaitValue())
    }

    @Test
    fun submitFeedback_setsFeedbackStatus() = runTest {
        // Step 1: create ViewModel with a fake repository.
        val vm = ScanViewModel(FakeRepo())

        // Step 2: send feedback.
        val feedback = FeedbackRequest(
            imageId = "img-123",
            userFeedback = true,
            timestamp = 123456789L,
        )
        vm.submitFeedback(feedback)

        // Step 3: feedback status should be success.
        val result = vm.feedbackStatus.getOrAwaitValueSkipNull()
        assertNotNull(result)
        assertTrue(result!!.isSuccess)
        assertEquals(true, result.getOrNull())
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
    // Observe until we get one value or timeout.
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
    // Observe until we get a non-null value or timeout.
    this.observeForever(observer)
    if (!latch.await(time, timeUnit)) {
        this.removeObserver(observer)
        throw TimeoutException("LiveData value was never set.")
    }
    return data
}
