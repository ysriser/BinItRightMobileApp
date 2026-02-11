package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Intent
import android.os.Looper
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class MainActivityTest {

    @Test
    fun onCreate_atLogin_hidesBottomNavigation() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()
        val bottomNav = activity.findViewById<BottomNavigationView>(R.id.bottom_nav_view)

        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(0, bottomNav.layoutParams.height)
        assertEquals(0f, bottomNav.alpha)
        assertEquals(0, bottomNav.menu.size())
    }

    @Test
    fun destinationChange_toHome_showsAndInflatesBottomNavigation() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()
        val navHost = activity.supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHost.navController
        val bottomNav = activity.findViewById<BottomNavigationView>(R.id.bottom_nav_view)

        navController.navigate(R.id.action_loginFragment_to_homeFragment)
        shadowOf(Looper.getMainLooper()).idle()

        assertTrue(bottomNav.menu.size() > 0)
        assertEquals(1f, bottomNav.alpha)
        assertEquals(ConstraintLayout.LayoutParams.WRAP_CONTENT, bottomNav.layoutParams.height)
    }

    @Test
    fun authReceiver_whenTriggered_navigatesToLoginAndShowsToast() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()
        val navHost = activity.supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHost.navController
        navController.navigate(R.id.action_loginFragment_to_homeFragment)
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(R.id.nav_home, navController.currentDestination?.id)

        val receiverField = MainActivity::class.java.getDeclaredField("authReceiver")
        receiverField.isAccessible = true
        val receiver = receiverField.get(activity) as BroadcastReceiver
        receiver.onReceive(activity, Intent("com.binitright.AUTH_FAILED"))
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(R.id.loginFragment, navController.currentDestination?.id)
        assertEquals("Session Expired. Please login again.", ShadowToast.getTextOfLatestToast())
    }
}
