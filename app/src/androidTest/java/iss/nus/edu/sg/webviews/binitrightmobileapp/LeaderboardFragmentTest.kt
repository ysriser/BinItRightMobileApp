package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.os.Bundle
import android.widget.FrameLayout
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.LeaderboardEntry
import iss.nus.edu.sg.webviews.binitrightmobileapp.network.RetrofitClient
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.not
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import retrofit2.Response

@RunWith(AndroidJUnit4::class)
class LeaderboardFragmentTest {

    private lateinit var mockApiService: ApiService

    @Before
    fun setup() {
        mockApiService = mock(ApiService::class.java)
        injectMockApiService(mockApiService)
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
    fun testLeaderboard_Success() {
        val fakeData = listOf(
            LeaderboardEntry(1, "Alice", 100),
            LeaderboardEntry(2, "Bob", 80)
        )
        val mockResponse = Response.success(fakeData)

        runBlocking {
            `when`(mockApiService.getLeaderboard()).thenReturn(mockResponse)
        }

        val scenario = launchFragmentInContainer<LeaderboardFragment>(
            themeResId = com.google.android.material.R.style.Theme_MaterialComponents_DayNight_NoActionBar
        )

        onView(withId(R.id.rv_leaderboard)).check(matches(isDisplayed()))

        scenario.onFragment { fragment ->
            val recyclerView = fragment.view?.findViewById<RecyclerView>(R.id.rv_leaderboard)
            val adapter = recyclerView?.adapter as? LeaderboardAdapter
            assert(adapter != null)
            assert(adapter?.itemCount == 2)
        }
    }

    @Test
    fun testLeaderboard_ApiError() {
        val mockResponse = Response.error<List<LeaderboardEntry>>(500, okhttp3.ResponseBody.create(null, ""))

        runBlocking {
            `when`(mockApiService.getLeaderboard()).thenReturn(mockResponse)
        }

        launchFragmentInContainer<LeaderboardFragment>(
            themeResId = com.google.android.material.R.style.Theme_MaterialComponents_DayNight_NoActionBar
        )

        onView(withText("Error: 500"))
            .inRoot(withDecorView(not(any())))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testLeaderboard_NetworkException() {
        runBlocking {
            `when`(mockApiService.getLeaderboard()).thenThrow(RuntimeException("Network Down"))
        }

        launchFragmentInContainer<LeaderboardFragment>(
            themeResId = com.google.android.material.R.style.Theme_MaterialComponents_DayNight_NoActionBar
        )

        onView(withText("Network Error: Network Down"))
            .inRoot(withDecorView(not(any())))
            .check(matches(isDisplayed()))
    }
}