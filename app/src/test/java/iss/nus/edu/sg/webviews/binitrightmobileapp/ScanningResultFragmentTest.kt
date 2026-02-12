package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.app.Application
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class ScanningResultFragmentTest {

    @Test
    fun displayResult_coversUncertainRecyclableAndNonRecyclableStates() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        val fragment = ScanningResultFragment().apply {
            arguments = Bundle().apply {
                putString("imageUri", "file:///tmp/sample.jpg")
                putSerializable(
                    "scanResult",
                    ScanResult(
                        category = "Plastic bottle",
                        recyclable = true,
                        confidence = 0.9f,
                        instruction = "Rinse and recycle"
                    )
                )
            }
        }
        activity.supportFragmentManager.beginTransaction()
            .add(android.R.id.content, fragment)
            .commitNow()

        callPrivate(
            fragment,
            "displayResult",
            ScanResult(
                category = "unknown item",
                recyclable = false,
                confidence = 0.2f,
                instructions = emptyList()
            )
        )
        assertEquals("Not sure", fragment.requireView().findViewById<TextView>(R.id.tvBadge).text.toString())
        val uncertainCta = fragment.requireView().findViewById<Button>(R.id.btnRecycle)
        assertTrue(!uncertainCta.isEnabled)
        assertEquals(
            activity.getString(R.string.scanning_recycle_cta_disabled),
            uncertainCta.text.toString()
        )

        callPrivate(
            fragment,
            "displayResult",
            ScanResult(
                category = "Plastic bottle",
                recyclable = true,
                confidence = 0.8f,
                instructions = listOf("Clean", "Dry"),
                instruction = "Clean and dry"
            )
        )
        assertEquals("Recyclable", fragment.requireView().findViewById<TextView>(R.id.tvBadge).text.toString())
        assertTrue(
            fragment.requireView().findViewById<TextView>(R.id.tvInstructionSteps).text.toString().contains("1. Clean")
        )

        callPrivate(
            fragment,
            "displayResult",
            ScanResult(
                category = "food waste",
                recyclable = false,
                confidence = 0.7f,
                instructions = emptyList(),
                instruction = null
            )
        )
        val root = fragment.requireView()
        assertEquals("Not Recyclable", root.findViewById<TextView>(R.id.tvBadge).text.toString())
        assertTrue(root.findViewById<TextView>(R.id.tvInstructionSteps).text.toString().contains("Follow local disposal guidance"))
    }

    @Test
    fun listeners_navigateToExpectedDestinations() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        val fragment = ScanningResultFragment().apply {
            arguments = Bundle().apply {
                putString("imageUri", "file:///tmp/sample.jpg")
                putSerializable(
                    "scanResult",
                    ScanResult(
                        category = "E-waste - Charger",
                        recyclable = false,
                        confidence = 0.6f,
                        instruction = "Bring to e-waste collection point",
                        instructions = listOf("Bring to collection point"),
                        binType = ""
                    )
                )
            }
        }
        activity.supportFragmentManager.beginTransaction()
            .add(android.R.id.content, fragment)
            .commitNow()

        val recycleNav = navFromHomeToScanningResult(activity)
        Navigation.setViewNavController(fragment.requireView(), recycleNav)
        fragment.requireView().findViewById<View>(R.id.btnRecycle).performClick()
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(R.id.nearByBinFragment, recycleNav.currentDestination?.id)
        val recycleArgs = recycleNav.currentBackStackEntry?.arguments
        assertEquals("EWASTE", recycleArgs?.getString("selectedBinType"))
        assertEquals("E-waste - Charger", recycleArgs?.getString("wasteCategory"))
        assertEquals("E-Waste", recycleArgs?.getString("mappedWasteCategory"))

        val homeNav = navFromHomeToScanningResult(activity)
        Navigation.setViewNavController(fragment.requireView(), homeNav)
        fragment.requireView().findViewById<View>(R.id.btnNotNow).performClick()
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(R.id.nav_home, homeNav.currentDestination?.id)

        val scanAgainNav = navFromHomeToScanningResult(activity)
        Navigation.setViewNavController(fragment.requireView(), scanAgainNav)
        fragment.requireView().findViewById<View>(R.id.btnScanAgain).performClick()
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(R.id.scanItemFragment, scanAgainNav.currentDestination?.id)
    }

    private fun navFromHomeToScanningResult(activity: FragmentActivity): TestNavHostController {
        return TestNavHostController(activity).apply {
            setGraph(R.navigation.nav_graph)
            setCurrentDestination(R.id.nav_home)
            navigate(R.id.action_home_to_scanItem)
            navigate(R.id.action_scanItemFragment_to_scanningResultFragment)
        }
    }

    private fun callPrivate(target: Any, methodName: String, vararg args: Any?): Any? {
        val method = target.javaClass.declaredMethods.first {
            it.name == methodName && it.parameterTypes.size == args.size
        }
        method.isAccessible = true
        return method.invoke(target, *args)
    }
}
