package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.isNotEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import com.google.android.material.R as MaterialR

@RunWith(AndroidJUnit4::class)
class AchievementDetailFragmentTest {

    @Before
    fun setup() {
        Intents.init()
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    @Test
    fun testLockedState_DisplaysCorrectly() {
        val bundle = Bundle().apply {
            putString("name", "Recycle Rookie")
            putString("description", "First item recycled")
            putString("criteria", "Recycle 1 item")
            putString("iconUrl", "http://example.com/icon.png")
            putBoolean("isUnlocked", false)
        }

        launchFragmentInContainer<AchievementDetailFragment>(
            fragmentArgs = bundle,
            themeResId = MaterialR.style.Theme_MaterialComponents_DayNight_NoActionBar
        )

        onView(withId(R.id.tvDetailName)).check(matches(withText("Recycle Rookie")))
        onView(withId(R.id.tvUnlockStatus)).check(matches(withText("LOCKED")))
        onView(withId(R.id.btnShare)).check(matches(withText("Keep Recycling to Unlock")))
        onView(withId(R.id.btnShare)).check(matches(isNotEnabled()))
        onView(withId(R.id.tvDetailUserName)).check(matches(withText("-")))
    }

    @Test
    fun testUnlockedState_AndShareFeature() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("APP_PREFS", Context.MODE_PRIVATE)
        prefs.edit().putString("JWT_TOKEN", "fake_token_for_test").commit()

        val bundle = Bundle().apply {
            putString("name", "Recycle Master")
            putString("description", "100 items recycled")
            putBoolean("isUnlocked", true)
        }

        launchFragmentInContainer<AchievementDetailFragment>(
            fragmentArgs = bundle,
            themeResId = MaterialR.style.Theme_MaterialComponents_DayNight_NoActionBar
        )

        onView(withId(R.id.tvUnlockStatus)).check(matches(withText("UNLOCKED")))
        onView(withId(R.id.btnShare)).check(matches(isEnabled()))

        onView(withId(R.id.btnShare)).perform(click())

        intended(allOf(
            hasAction(Intent.ACTION_CHOOSER),
            hasExtra(
                Intent.EXTRA_INTENT,
                allOf(
                    hasAction(Intent.ACTION_SEND),
                    hasExtra(Intent.EXTRA_SUBJECT, "My Recycling Achievement")
                )
            )
        ))
    }
}