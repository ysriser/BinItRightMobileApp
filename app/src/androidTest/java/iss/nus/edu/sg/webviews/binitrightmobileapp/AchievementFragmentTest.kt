package iss.nus.edu.sg.webviews.binitrightmobileapp

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.Navigation
import androidx.navigation.NavController
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AchievementsFragmentTest {

    @Test
    fun testAchievementsFragmentDisplayAndNavigation() {
        val scenario = launchFragmentInContainer<AchievementsFragment>(
            themeResId = androidx.appcompat.R.style.Theme_AppCompat
        )

        val mockNavController = mockk<NavController>(relaxed = true)

        scenario.onFragment { fragment ->
            Navigation.setViewNavController(fragment.requireView(), mockNavController)
        }

        onView(withId(R.id.rvAchievements)).check(matches(isDisplayed()))
        onView(withId(R.id.progressBarOverall)).check(matches(isDisplayed()))
        onView(withId(R.id.tvPageTitle)).check(matches(isDisplayed()))

        onView(withId(R.id.btnBack)).perform(click())

        verify { mockNavController.popBackStack() }
    }
}