package iss.nus.edu.sg.webviews.binitrightmobileapp

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import com.google.android.material.R as MaterialR

@RunWith(AndroidJUnit4::class)
class AchievementsFragmentTest {

    @Test
    fun testAchievementsFragmentDisplay() {
        launchFragmentInContainer<AchievementsFragment>(
            themeResId = MaterialR.style.Theme_MaterialComponents_DayNight_NoActionBar
        )

        onView(withId(R.id.rvAchievements)).check(matches(isDisplayed()))
        onView(withId(R.id.progressBarOverall)).check(matches(isDisplayed()))
        onView(withId(R.id.tvPageTitle)).check(matches(isDisplayed()))
    }
}