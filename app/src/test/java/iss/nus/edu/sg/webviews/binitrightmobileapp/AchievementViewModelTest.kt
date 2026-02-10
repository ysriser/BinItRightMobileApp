package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.app.Application
import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.Achievement
import iss.nus.edu.sg.webviews.binitrightmobileapp.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import retrofit2.Response
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class AchievementViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {

        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun injectMockApiService(mockService: ApiService) {
        try {
            val field = RetrofitClient::class.java.getDeclaredField("api")
            field.isAccessible = true

            field.set(RetrofitClient, mockService)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Test
    fun fetchAchievements_FullCoverage_Success() {
        val app = RuntimeEnvironment.getApplication() as Application
        val prefs = app.getSharedPreferences("APP_PREFS", Context.MODE_PRIVATE)
        prefs.edit().putLong("USER_ID", 123L).commit()

        val mockApi = mock(ApiService::class.java)
        val fakeData = listOf(Achievement(1L, "First Submission", "", "", "", true))
        val mockResponse = Response.success(fakeData)

        runBlocking {
            `when`(mockApi.getAchievementsWithStatus(anyLong())).thenReturn(mockResponse)
        }

        injectMockApiService(mockApi)

        val vm = AchievementViewModel(app)

        val result = vm.achievementList.getOrAwaitValue()

        assertNotNull(result)
        assertTrue(result.find { it.id == 1L }?.isUnlocked == true)
        assertEquals(false, vm.isLoading.value)
    }

    @Test
    fun fetchAchievements_Coverage_Failure() {
        val app = RuntimeEnvironment.getApplication() as Application
        val prefs = app.getSharedPreferences("APP_PREFS", Context.MODE_PRIVATE)
        prefs.edit().putLong("USER_ID", 123L).commit()

        val mockApi = mock(ApiService::class.java)
        runBlocking {
            `when`(mockApi.getAchievementsWithStatus(anyLong())).thenThrow(RuntimeException("Network Down"))
        }

        injectMockApiService(mockApi)
        val vm = AchievementViewModel(app)

        val result = vm.achievementList.getOrAwaitValue()

        assertEquals(10, result.size)
        assertEquals(false, vm.isLoading.value)
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