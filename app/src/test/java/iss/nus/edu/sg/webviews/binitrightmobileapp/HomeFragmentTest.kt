package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.app.Application
import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.fragment.app.FragmentActivity
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.UserProfile
import iss.nus.edu.sg.webviews.binitrightmobileapp.network.RetrofitClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import retrofit2.Response
import java.lang.reflect.Proxy

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class HomeFragmentTest {

    @Test
    fun onViewCreated_withMissingUserId_keepsFragmentStable() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        activity.getSharedPreferences("APP_PREFS", 0).edit().clear().apply()

        val fragment = HomeFragment()
        activity.supportFragmentManager.beginTransaction()
            .add(android.R.id.content, fragment)
            .commitNow()

        assertNotNull(fragment.view?.findViewById(R.id.btnScan))
        assertNotNull(fragment.view?.findViewById(R.id.aiSummary))
    }

    @Test
    fun fetchUserStats_apiNotInitialized_setsFallbackSummary() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        activity.getSharedPreferences("APP_PREFS", 0)
            .edit()
            .putLong("USER_ID", 42L)
            .apply()
        installApiServiceStub { method ->
            if (method == "getProfileSummary") {
                throw IllegalStateException("forced failure")
            }
            throw UnsupportedOperationException(method)
        }

        val fragment = HomeFragment()
        activity.supportFragmentManager.beginTransaction()
            .add(android.R.id.content, fragment)
            .commitNow()

        shadowOf(Looper.getMainLooper()).idle()

        val summary = fragment.requireView().findViewById<TextView>(R.id.aiSummary).text.toString()
        assertEquals(
            "You're making a positive environmental impact. Keep recycling!",
            summary
        )
    }

    @Test
    fun fetchUserStats_success_updatesStatsOnScreen() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        activity.getSharedPreferences("APP_PREFS", 0)
            .edit()
            .putLong("USER_ID", 88L)
            .apply()
        installApiServiceStub { method ->
            if (method == "getProfileSummary") {
                Response.success(
                    UserProfile(
                        name = "Tester",
                        pointBalance = 321,
                        equippedAvatarName = "default",
                        totalRecycled = 17,
                        aiSummary = "Great consistency",
                        totalAchievement = 9,
                        carbonEmissionSaved = 12.3
                    )
                )
            } else {
                throw UnsupportedOperationException(method)
            }
        }

        val fragment = HomeFragment()
        activity.supportFragmentManager.beginTransaction()
            .add(android.R.id.content, fragment)
            .commitNow()
        shadowOf(Looper.getMainLooper()).idle()

        val root = fragment.requireView()
        assertEquals("321", root.findViewById<TextView>(R.id.tvPointsCount).text.toString())
        assertEquals("17", root.findViewById<TextView>(R.id.tvRecycledCount).text.toString())
        assertEquals("9", root.findViewById<TextView>(R.id.tvAchievementCount).text.toString())
        assertEquals("12.3 kg", root.findViewById<TextView>(R.id.tvCo2Saved).text.toString())
        assertEquals("Great consistency", root.findViewById<TextView>(R.id.aiSummary).text.toString())
    }

    @Test
    fun clickActions_withNavController_navigateToExpectedDestinations() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        activity.getSharedPreferences("APP_PREFS", 0).edit().putLong("USER_ID", -1L).apply()
        val fragment = HomeFragment()
        activity.supportFragmentManager.beginTransaction()
            .add(android.R.id.content, fragment)
            .commitNow()
        val navController = TestNavHostController(activity).apply {
            setGraph(R.navigation.nav_graph)
            setCurrentDestination(R.id.nav_home)
        }
        Navigation.setViewNavController(fragment.requireView(), navController)

        val checks = listOf(
            R.id.btnScan to R.id.scanItemFragment,
            R.id.btnQuiz to R.id.questionnaireFragment,
            R.id.cardFindBins to R.id.findRecyclingBinFragment,
            R.id.btnRecycleNow to R.id.scanHomeFragment,
            R.id.cardChatHelper to R.id.chatFragment,
            R.id.cardAchievements to R.id.achievementsFragment
        )

        checks.forEach { (viewId, expectedDestination) ->
            navController.setCurrentDestination(R.id.nav_home)
            fragment.requireView().findViewById<View>(viewId).performClick()
            assertEquals(expectedDestination, navController.currentDestination?.id)
        }
    }

    @Test
    fun reportIssueClick_showsDialog_andOnDestroyView_clearsBinding() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        val fragment = HomeFragment()
        activity.supportFragmentManager.beginTransaction()
            .add(android.R.id.content, fragment)
            .commitNow()

        fragment.requireView().findViewById<View>(R.id.cardReportIssue).performClick()
        shadowOf(Looper.getMainLooper()).idle()

        fragment.onDestroyView()
        assertNull(getPrivateField(fragment, "_binding"))
    }

    private fun getPrivateField(target: Any, fieldName: String): Any? {
        val field = target.javaClass.getDeclaredField(fieldName)
        field.isAccessible = true
        return field.get(target)
    }

    private fun installApiServiceStub(handler: (methodName: String) -> Any?) {
        val proxy = Proxy.newProxyInstance(
            ApiService::class.java.classLoader,
            arrayOf(ApiService::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "toString" -> "ApiServiceStub"
                "hashCode" -> 0
                "equals" -> false
                else -> handler(method.name)
            }
        } as ApiService

        val apiField = RetrofitClient::class.java.getDeclaredField("api")
        apiField.isAccessible = true
        apiField.set(RetrofitClient, proxy)
    }
}
