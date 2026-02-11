package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.app.Application
import android.content.Context
import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.Achievement
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.LeaderboardEntry
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.UserProfile
import iss.nus.edu.sg.webviews.binitrightmobileapp.network.RetrofitClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
class ProfileFragmentTest {

    @Test
    fun loadProfileData_success_updatesSummaryAndRank() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        activity.getSharedPreferences("APP_PREFS", Context.MODE_PRIVATE)
            .edit()
            .putLong("USER_ID", 9L)
            .apply()

        installApiServiceStub { methodName, _ ->
            when (methodName) {
                "getProfileSummary" -> Response.success(
                    UserProfile(
                        name = "Alice",
                        pointBalance = 345,
                        equippedAvatarName = "not_found_avatar",
                        totalRecycled = 23,
                        aiSummary = "summary",
                        totalAchievement = 4,
                        carbonEmissionSaved = 7.8
                    )
                )
                "getAchievementsWithStatus" -> Response.success(
                    listOf(
                        Achievement(1, "A", "d", "c", "icon", isUnlocked = true),
                        Achievement(2, "B", "d", "c", "icon", isUnlocked = false)
                    )
                )
                "getLeaderboard" -> Response.success(
                    listOf(
                        LeaderboardEntry(9L, "Alice", 100),
                        LeaderboardEntry(10L, "Bob", 80)
                    )
                )
                else -> throw UnsupportedOperationException(methodName)
            }
        }

        val fragment = ProfileFragment()
        activity.supportFragmentManager.beginTransaction()
            .add(android.R.id.content, fragment)
            .commitNow()

        waitMainUntil {
            fragment.requireView().findViewById<TextView>(R.id.profileName).text.toString() == "Alice"
        }

        val root = fragment.requireView()
        assertEquals("Alice", root.findViewById<TextView>(R.id.profileName).text.toString())
        assertEquals("345 Points", root.findViewById<TextView>(R.id.pointsDisplay).text.toString())
        assertEquals("23", root.findViewById<TextView>(R.id.summaryRecycled).text.toString())
        assertEquals("345", root.findViewById<TextView>(R.id.gridPoints).text.toString())
        assertEquals("23", root.findViewById<TextView>(R.id.gridItems).text.toString())
        assertEquals("1", root.findViewById<TextView>(R.id.gridAwards).text.toString())
        assertEquals("1", root.findViewById<TextView>(R.id.summaryBadges).text.toString())
        assertEquals("#1", root.findViewById<TextView>(R.id.gridRank).text.toString())
    }

    @Test
    fun loadProfileData_whenUserMissing_skipsDependentRequests() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        activity.getSharedPreferences("APP_PREFS", Context.MODE_PRIVATE)
            .edit()
            .putLong("USER_ID", -1L)
            .apply()

        val calls = mutableMapOf<String, Int>()
        installApiServiceStub { methodName, _ ->
            calls[methodName] = (calls[methodName] ?: 0) + 1
            when (methodName) {
                "getProfileSummary" -> Response.success(
                    UserProfile(
                        name = "NoUser",
                        pointBalance = 10,
                        equippedAvatarName = "default",
                        totalRecycled = 2,
                        aiSummary = "",
                        totalAchievement = 0,
                        carbonEmissionSaved = 0.0
                    )
                )
                else -> throw UnsupportedOperationException(methodName)
            }
        }

        val fragment = ProfileFragment()
        activity.supportFragmentManager.beginTransaction()
            .add(android.R.id.content, fragment)
            .commitNow()

        waitMainUntil {
            fragment.requireView().findViewById<TextView>(R.id.profileName).text.toString() == "NoUser"
        }

        assertEquals(1, calls["getProfileSummary"])
        assertTrue((calls["getAchievementsWithStatus"] ?: 0) == 0)
        assertTrue((calls["getLeaderboard"] ?: 0) == 0)
    }

    @Test
    fun cardClicks_andLogout_navigateToExpectedDestinations() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        activity.getSharedPreferences("APP_PREFS", Context.MODE_PRIVATE)
            .edit()
            .putLong("USER_ID", -1L)
            .putString("TOKEN", "token-123")
            .apply()

        installApiServiceStub { methodName, _ ->
            when (methodName) {
                "getProfileSummary" -> Response.success(
                    UserProfile(
                        name = "Tester",
                        pointBalance = 0,
                        equippedAvatarName = "default",
                        totalRecycled = 0,
                        aiSummary = "",
                        totalAchievement = 0,
                        carbonEmissionSaved = 0.0
                    )
                )
                else -> throw UnsupportedOperationException(methodName)
            }
        }

        val fragment = ProfileFragment()
        activity.supportFragmentManager.beginTransaction()
            .add(android.R.id.content, fragment)
            .commitNow()

        val checks = listOf(
            R.id.leaderboardCard to R.id.leaderboardFragment,
            R.id.rewardShopCard to R.id.RewardShopFragment,
            R.id.recycle_history to R.id.recycleHistoryFragment,
            R.id.customizeAvatarBtn to R.id.avatarCustomizationFragment,
            R.id.achievementsCard to R.id.achievementsFragment
        )

        checks.forEach { (viewId, expectedDest) ->
            val navController = TestNavHostController(activity).apply {
                setGraph(R.navigation.nav_graph)
                setCurrentDestination(R.id.nav_profile)
            }
            Navigation.setViewNavController(fragment.requireView(), navController)
            fragment.requireView().findViewById<View>(viewId).performClick()
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals(expectedDest, navController.currentDestination?.id)
        }

        val logoutNav = TestNavHostController(activity).apply {
            setGraph(R.navigation.nav_graph)
            setCurrentDestination(R.id.nav_profile)
        }
        Navigation.setViewNavController(fragment.requireView(), logoutNav)
        fragment.requireView().findViewById<View>(R.id.logoutBtn).performClick()
        shadowOf(Looper.getMainLooper()).idle()

        val token = activity.getSharedPreferences("APP_PREFS", Context.MODE_PRIVATE)
            .getString("TOKEN", null)
        assertEquals(null, token)
        assertEquals(R.id.loginFragment, logoutNav.currentDestination?.id)
    }

    private fun installApiServiceStub(handler: (methodName: String, args: Array<Any?>?) -> Any?) {
        val proxy = Proxy.newProxyInstance(
            ApiService::class.java.classLoader,
            arrayOf(ApiService::class.java)
        ) { _, method, args ->
            when (method.name) {
                "toString" -> "ApiServiceStub"
                "hashCode" -> 0
                "equals" -> false
                else -> handler(method.name, args)
            }
        } as ApiService

        val apiField = RetrofitClient::class.java.getDeclaredField("api")
        apiField.isAccessible = true
        apiField.set(RetrofitClient, proxy)
    }

    private fun waitMainUntil(loops: Int = 20, condition: () -> Boolean) {
        repeat(loops) {
            if (condition()) return
            shadowOf(Looper.getMainLooper()).idle()
        }
    }
}
