package iss.nus.edu.sg.webviews.binitrightmobileapp

/*
 * File purpose:
 * - Unit tests for ScanViewModel core behavior.
 * - Uses fake repositories so tests are deterministic and fast.
 * - Covers success, failure, debug-mode forcing, and reset/feedback states.
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
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScanViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    private class FakeRepo : ScanRepository {
        override suspend fun scanImage(
            imageFile: File,
            forceTier2: Boolean,
            onStatusUpdate: (String) -> Unit,
        ): Result<ScanResult> {
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

    private class ForceTier2CaptureRepo : ScanRepository {
        var lastForceTier2: Boolean? = null

        override suspend fun scanImage(
            imageFile: File,
            forceTier2: Boolean,
            onStatusUpdate: (String) -> Unit,
        ): Result<ScanResult> {
            lastForceTier2 = forceTier2
            onStatusUpdate("Analyzing...")
            return Result.success(
                ScanResult(
                    category = "Metal",
                    recyclable = true,
                    confidence = 0.88f,
                    instruction = "Empty and rinse.",
                    instructions = listOf("Empty", "Rinse", "Recycle"),
                )
            )
        }

        override suspend fun sendFeedback(feedback: FeedbackRequest): Result<Boolean> {
            return Result.success(true)
        }
    }

    private class FailingRepo : ScanRepository {
        override suspend fun scanImage(
            imageFile: File,
            forceTier2: Boolean,
            onStatusUpdate: (String) -> Unit,
        ): Result<ScanResult> {
            return Result.failure(IllegalStateException("boom"))
        }

        override suspend fun sendFeedback(feedback: FeedbackRequest): Result<Boolean> {
            return Result.success(true)
        }
    }

    @Test
    fun scanImage_setsResult() = runTest {
        val vm = ScanViewModel(FakeRepo())

        assertFalse(vm.isLoading.getOrAwaitValue())

        vm.scanImage(File("dummy.jpg"))

        val result = vm.scanResult.getOrAwaitValueSkipNull()
        assertNotNull(result)
        assertTrue(result!!.isSuccess)
        assertEquals("Glass", result.getOrNull()?.category)
        assertFalse(vm.isLoading.getOrAwaitValue())
    }

    @Test
    fun scanImage_inDebugMode_passesForceTier2True() = runTest {
        val repo = ForceTier2CaptureRepo()
        val vm = ScanViewModel(repo)

        vm.toggleDebugMode(true)
        assertTrue(vm.isDebugMode.getOrAwaitValue())

        vm.scanImage(File("dummy.jpg"))

        val result = vm.scanResult.getOrAwaitValueSkipNull()
        assertTrue(result!!.isSuccess)
        assertEquals(true, repo.lastForceTier2)
    }

    @Test
    fun scanImage_failure_setsFailureResult_andStopsLoading() = runTest {
        val vm = ScanViewModel(FailingRepo())

        vm.scanImage(File("dummy.jpg"))

        val result = vm.scanResult.getOrAwaitValueSkipNull()
        assertTrue(result!!.isFailure)
        assertEquals("boom", result.exceptionOrNull()?.message)
        assertFalse(vm.isLoading.getOrAwaitValue())
    }

    @Test
    fun resetScanState_clearsResult() = runTest {
        val vm = ScanViewModel(FakeRepo())

        vm.scanImage(File("dummy.jpg"))
        assertNotNull(vm.scanResult.getOrAwaitValueSkipNull())

        vm.resetScanState()
        assertNull(vm.scanResult.getOrAwaitValue())
    }

    @Test
    fun submitFeedback_setsFeedbackStatus() = runTest {
        val vm = ScanViewModel(FakeRepo())

        val feedback = FeedbackRequest(
            imageId = "img-123",
            userFeedback = true,
            timestamp = 123456789L,
        )
        vm.submitFeedback(feedback)

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
