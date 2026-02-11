package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.app.Application
import android.os.Looper
import android.view.View
import androidx.fragment.app.FragmentActivity
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class ScanHomeFragmentTest {

    @Test
    fun clickActions_navigateToExpectedDestinations() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        val fragment = ScanHomeFragment()
        activity.supportFragmentManager.beginTransaction()
            .add(android.R.id.content, fragment)
            .commitNow()

        val checks = listOf(
            R.id.btnStartScan to R.id.scanItemFragment,
            R.id.btnQuestionnaire to R.id.questionnaireFragment,
            R.id.btnYesIKnow to R.id.findRecyclingBinFragment
        )

        checks.forEach { (viewId, expectedDest) ->
            val navController = TestNavHostController(activity).apply {
                setGraph(R.navigation.nav_graph)
                setCurrentDestination(R.id.scanHomeFragment)
            }
            Navigation.setViewNavController(fragment.requireView(), navController)
            fragment.requireView().findViewById<View>(viewId).performClick()
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals(expectedDest, navController.currentDestination?.id)
        }
    }

    @Test
    fun backClick_andDestroyView_keepFragmentStableAndClearBinding() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        val fragment = ScanHomeFragment()
        activity.supportFragmentManager.beginTransaction()
            .add(android.R.id.content, fragment)
            .commitNow()

        val navController = TestNavHostController(activity).apply {
            setGraph(R.navigation.nav_graph)
            setCurrentDestination(R.id.scanHomeFragment)
        }
        Navigation.setViewNavController(fragment.requireView(), navController)
        fragment.requireView().findViewById<View>(R.id.btnBackToHome).performClick()
        shadowOf(Looper.getMainLooper()).idle()
        assertNotNull(navController.currentDestination)

        fragment.onDestroyView()
        assertNull(getPrivateField(fragment, "_binding"))
    }

    private fun getPrivateField(target: Any, fieldName: String): Any? {
        val field = target.javaClass.getDeclaredField(fieldName)
        field.isAccessible = true
        return field.get(target)
    }
}
