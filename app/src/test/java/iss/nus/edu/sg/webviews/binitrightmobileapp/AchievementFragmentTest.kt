package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.view.View
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.Achievement
import iss.nus.edu.sg.webviews.binitrightmobileapp.databinding.FragmentAchievementsBinding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AchievementsFragmentTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Test
    fun testAchievementsFragmentFullCoverage() {
        val mockViewModel = mock(AchievementViewModel::class.java)
        val liveData = MutableLiveData<List<Achievement>>()
        `when`(mockViewModel.achievementList).thenReturn(liveData)

        val scenario = launchFragmentInContainer<AchievementsFragment>(
            themeResId = R.style.Theme_BinItRightMobileApp
        )

        scenario.onFragment { fragment ->
            // 注意：如果还报错 NoSuchFieldException，请把下面的 "viewModel" 改为 "achievementViewModel"
            val vmField = AchievementsFragment::class.java.getDeclaredFields().firstOrNull {
                it.type == AchievementViewModel::class.java
            } ?: throw NoSuchFieldException("Could not find AchievementViewModel field")

            vmField.isAccessible = true
            vmField.set(fragment, mockViewModel)

            val bindingField = AchievementsFragment::class.java.getDeclaredFields().firstOrNull {
                it.type.name.contains("FragmentAchievementsBinding")
            } ?: throw NoSuchFieldException("Could not find Binding field")

            bindingField.isAccessible = true
            val binding = bindingField.get(fragment) as? FragmentAchievementsBinding

            assertNotNull(binding)

            val navController = mock(NavController::class.java)
            Navigation.setViewNavController(fragment.requireView(), navController)

            liveData.value = listOf(
                Achievement(1L, "A", "D", "C", "U", true),
                Achievement(2L, "B", "D", "C", "U", false)
            )
            assertEquals("1/2", binding?.tvProgressFraction?.text.toString())
            assertEquals("1 more to unlock!", binding?.tvProgressMessage?.text.toString())

            liveData.value = listOf(Achievement(1L, "A", "D", "C", "U", true))
            assertEquals("All achievements completed! 🎉", binding?.tvProgressMessage?.text.toString())

            binding?.btnBack?.performClick()
            verify(navController).popBackStack()

            val recyclerView = binding?.rvAchievements
            recyclerView?.measure(0, 0)
            recyclerView?.layout(0, 0, 1000, 1000)
            recyclerView?.findViewHolderForAdapterPosition(0)?.itemView?.performClick()
            verify(navController).navigate(any(NavDirections::class.java))
        }

        scenario.moveToState(Lifecycle.State.DESTROYED)
    }
}