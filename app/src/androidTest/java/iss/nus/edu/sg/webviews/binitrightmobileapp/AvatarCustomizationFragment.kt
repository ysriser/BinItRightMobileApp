package iss.nus.edu.sg.webviews.binitrightmobileapp

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.Accessory
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.UserAccessory
import iss.nus.edu.sg.webviews.binitrightmobileapp.network.RetrofitClient
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.Mockito.times
import retrofit2.Response

@RunWith(AndroidJUnit4::class)
class AvatarCustomizationFragmentTest {

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
    fun testLoadAccessories_Success() {
        val fakeAccessories = listOf(
            UserAccessory(1, true, Accessory(101L, "Blue Cap", "img_url_1", 100)),
            UserAccessory(2, false, Accessory(102L, "Red Shirt", "img_url_2", 200))
        )
        val mockResponse = Response.success(fakeAccessories)

        runBlocking {
            `when`(mockApiService.getMyAccessories()).thenReturn(mockResponse)
        }

        val scenario = launchFragmentInContainer<AvatarCustomizationFragment>(
            themeResId = com.google.android.material.R.style.Theme_MaterialComponents_DayNight_NoActionBar
        )

        onView(withId(R.id.accessoriesGrid)).check(matches(isDisplayed()))

        scenario.onFragment { fragment ->
            val recyclerView = fragment.view?.findViewById<RecyclerView>(R.id.accessoriesGrid)
            val adapter = recyclerView?.adapter
            assert(adapter != null)
            assert(adapter?.itemCount == 2)
        }
    }

    @Test
    fun testEquipAccessory_Success() {
        val initialList = listOf(
            UserAccessory(1, false, Accessory(101L, "Blue Cap", "img_url_1", 100))
        )
        val updatedList = listOf(
            UserAccessory(1, true, Accessory(101L, "Blue Cap", "img_url_1", 100))
        )

        runBlocking {
            `when`(mockApiService.getMyAccessories())
                .thenReturn(Response.success(initialList))
                .thenReturn(Response.success(updatedList))

            `when`(mockApiService.equipAccessory(101L))
                .thenReturn(Response.success(null))
        }

        launchFragmentInContainer<AvatarCustomizationFragment>(
            themeResId = com.google.android.material.R.style.Theme_MaterialComponents_DayNight_NoActionBar
        )

        onView(withId(R.id.accessoriesGrid))
            .perform(RecyclerViewActions.actionOnItemAtPosition<AvatarAdapter.ViewHolder>(0, click()))

        runBlocking {
            verify(mockApiService).equipAccessory(101L)
            verify(mockApiService, times(2)).getMyAccessories()
        }
    }
}