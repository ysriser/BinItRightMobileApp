package iss.nus.edu.sg.webviews.binitrightmobileapp

/*
 * File purpose:
 * - Unit tests for ScanViewModel.
 * - Uses fake repository for deterministic behavior.
 * - Covers scan state, debug mode forceTier2, reset and feedback.
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
        var lastForceTier2: Boolean = false

        override suspend fun scanImage(
            imageFile: File,
            forceTier2: Boolean,
            onStatusUpdate: (String) -> Unit,
        ): Result<ScanResult> {
            lastForceTier2 = forceTier2
            onStatusUpdate("Analyzing...")
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
    fun debugMode_true_passesForceTier2ToRepository() = runTest {
        val repo = FakeRepo()
        val vm = ScanViewModel(repo)

        vm.toggleDebugMode(true)
        vm.scanImage(File("dummy.jpg"))
        vm.scanResult.getOrAwaitValueSkipNull()

        assertTrue(repo.lastForceTier2)
    }

    @Test
    fun debugMode_false_keepsTier1FirstFlow() = runTest {
        val repo = FakeRepo()
        val vm = ScanViewModel(repo)

        vm.toggleDebugMode(false)
        vm.scanImage(File("dummy.jpg"))
        vm.scanResult.getOrAwaitValueSkipNull()

        assertFalse(repo.lastForceTier2)
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