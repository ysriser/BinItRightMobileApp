package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.app.Application
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class AchievementsFragmentTest {

    @Test
    fun launchFragment_withViewModelFactory_doesNotCrash() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        val fragment = AchievementsFragment()

        activity.supportFragmentManager.beginTransaction()
            .add(android.R.id.content, fragment)
            .commitNow()

        val recyclerView = fragment.requireView().findViewById<RecyclerView>(R.id.rvAchievements)
        assertNotNull(recyclerView)
    }
}
