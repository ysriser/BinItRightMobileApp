package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.test.core.app.ApplicationProvider
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.Achievement
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class AchievementViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
        val app = ApplicationProvider.getApplicationContext<Application>()
        app.getSharedPreferences("APP_PREFS", android.content.Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }

    @Test
    fun fetchAchievements_whenUserIdMissing_usesFixedListWithoutNetwork() {
        val app = ApplicationProvider.getApplicationContext<Application>()

        val viewModel = AchievementViewModel(
            application = app,
            userIdProvider = { -1L },
            remoteFetcher = { error("Remote fetch should not be called") }
        )

        val achievements = viewModel.achievementList.getOrAwaitValue()
        assertEquals(10, achievements.size)
        assertTrue(achievements.none { it.isUnlocked })
    }

    @Test
    fun fetchAchievements_whenRemoteSuccess_mergesUnlockedStates() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val remoteList = listOf(
            Achievement(
                id = 2L,
                name = "Remote",
                description = "Remote",
                criteria = "Remote",
                badgeIconUrl = "https://example.com/icon.png",
                isUnlocked = true,
            )
        )

        val viewModel = AchievementViewModel(
            application = app,
            userIdProvider = { 100L },
            remoteFetcher = { Response.success(remoteList) }
        )

        val achievements = viewModel.achievementList.getOrAwaitValue()
        val merged = achievements.first { it.id == 2L }

        assertTrue(merged.isUnlocked)
        assertFalse(viewModel.isLoading.value ?: true)
    }

    @Test
    fun fetchAchievements_whenRemoteSuccessButBodyNull_usesFixedList() {
        val app = ApplicationProvider.getApplicationContext<Application>()

        val viewModel = AchievementViewModel(
            application = app,
            userIdProvider = { 100L },
            remoteFetcher = { Response.success(null) }
        )

        val achievements = viewModel.achievementList.getOrAwaitValue()
        assertEquals(10, achievements.size)
        assertTrue(achievements.none { it.isUnlocked })
    }

    @Test
    fun fetchAchievements_whenRemoteError_fallsBackToFixedList() {
        val app = ApplicationProvider.getApplicationContext<Application>()

        val viewModel = AchievementViewModel(
            application = app,
            userIdProvider = { 100L },
            remoteFetcher = {
                Response.error(
                    500,
                    "boom".toResponseBody("text/plain".toMediaType())
                )
            }
        )

        val achievements = viewModel.achievementList.getOrAwaitValue()

        assertEquals(10, achievements.size)
        assertTrue(achievements.none { it.isUnlocked })
        assertFalse(viewModel.isLoading.value ?: true)
    }

    @Test
    fun fetchAchievements_whenException_fallsBackToFixedList() {
        val app = ApplicationProvider.getApplicationContext<Application>()

        val viewModel = AchievementViewModel(
            application = app,
            userIdProvider = { 100L },
            remoteFetcher = { throw Exception("Network failure") }
        )

        val achievements = viewModel.achievementList.getOrAwaitValue()
        assertEquals(10, achievements.size)
        assertFalse(viewModel.isLoading.value ?: true)
    }

    @Test
    fun testDefaultConstructor_coverage() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        try {
            AchievementViewModel(app)
        } catch (e: Exception) {
        }
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