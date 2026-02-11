package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.app.Application
import android.os.Bundle
import android.os.Looper
import android.view.View
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
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class QuestionnaireResultFragmentTest {

    @Test
    fun onViewCreated_withoutOutcome_showsErrorToast() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        val fragment = QuestionnaireResultFragment()
        activity.supportFragmentManager.beginTransaction()
            .add(android.R.id.content, fragment)
            .commitNow()

        shadowOf(Looper.getMainLooper()).idle()
        assertEquals("Error loading result", ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun setupUi_coversUncertainRecyclableAndNonRecyclableBranches() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        val fragment = QuestionnaireResultFragment().apply {
            arguments = Bundle().apply {
                putSerializable(
                    "outcome",
                    SerializableOutcome(
                        categoryTitle = "Plastic",
                        disposalLabel = "Recyclable",
                        certainty = "HIGH",
                        explanation = "ok",
                        tips = listOf("Clean it")
                    )
                )
            }
        }
        activity.supportFragmentManager.beginTransaction()
            .add(android.R.id.content, fragment)
            .commitNow()

        callPrivate(
            fragment,
            "setupUI",
            SerializableOutcome(
                categoryTitle = "Unknown item",
                disposalLabel = "Unknown",
                certainty = "LOW",
                explanation = "not sure",
                tips = listOf("Check local guide")
            )
        )
        assertEquals("Not sure", fragment.requireView().findViewById<TextView>(R.id.tvBadge).text.toString())

        callPrivate(
            fragment,
            "setupUI",
            SerializableOutcome(
                categoryTitle = "Plastic bottle",
                disposalLabel = "Recyclable",
                certainty = "HIGH",
                explanation = "yes",
                tips = listOf("Rinse", "Dry")
            )
        )
        assertEquals("Recyclable", fragment.requireView().findViewById<TextView>(R.id.tvBadge).text.toString())

        callPrivate(
            fragment,
            "setupUI",
            SerializableOutcome(
                categoryTitle = "Food waste",
                disposalLabel = "General waste",
                certainty = "HIGH",
                explanation = "no",
                tips = listOf("Dispose safely"),
                instruction = "Put into general waste"
            )
        )
        val root = fragment.requireView()
        assertEquals("Not Recyclable", root.findViewById<TextView>(R.id.tvBadge).text.toString())
        assertEquals("Put into general waste", root.findViewById<TextView>(R.id.tvTips).text.toString())
    }

    @Test
    fun listeners_navigateAndHandleFeedback() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        val fragment = QuestionnaireResultFragment().apply {
            arguments = Bundle().apply {
                putSerializable(
                    "outcome",
                    SerializableOutcome(
                        categoryTitle = "E-waste - Charger",
                        disposalLabel = "Not Recyclable",
                        certainty = "MEDIUM",
                        explanation = "special stream",
                        tips = listOf("Bring to e-waste collection point")
                    )
                )
            }
        }
        activity.supportFragmentManager.beginTransaction()
            .add(android.R.id.content, fragment)
            .commitNow()

        val scanNav = navFromHomeToQuestionnaireResult(activity)
        Navigation.setViewNavController(fragment.requireView(), scanNav)
        fragment.requireView().findViewById<View>(R.id.btnTryAiScan).performClick()
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(R.id.scanItemFragment, scanNav.currentDestination?.id)

        val recycleNav = navFromHomeToQuestionnaireResult(activity)
        Navigation.setViewNavController(fragment.requireView(), recycleNav)
        fragment.requireView().findViewById<View>(R.id.btnRecycle).performClick()
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(R.id.nearByBinFragment, recycleNav.currentDestination?.id)
        val recycleArgs = recycleNav.currentBackStackEntry?.arguments
        assertEquals("EWASTE", recycleArgs?.getString("selectedBinType"))
        assertEquals("E-waste - Charger", recycleArgs?.getString("wasteCategory"))
        assertEquals("E-Waste", recycleArgs?.getString("mappedWasteCategory"))

        val backNav = navFromHomeToQuestionnaireResult(activity)
        Navigation.setViewNavController(fragment.requireView(), backNav)
        fragment.requireView().findViewById<View>(R.id.btnScanAgain).performClick()
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(R.id.questionnaireFragment, backNav.currentDestination?.id)

        val fallbackBackNav = TestNavHostController(activity).apply {
            setGraph(R.navigation.nav_graph)
            setCurrentDestination(R.id.questionnaireResultFragment)
        }
        Navigation.setViewNavController(fragment.requireView(), fallbackBackNav)
        fragment.requireView().findViewById<View>(R.id.btnScanAgain).performClick()
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(R.id.questionnaireFragment, fallbackBackNav.currentDestination?.id)

        val homeNav = navFromHomeToQuestionnaireResult(activity)
        Navigation.setViewNavController(fragment.requireView(), homeNav)
        fragment.requireView().findViewById<View>(R.id.btnNotNow).performClick()
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(R.id.nav_home, homeNav.currentDestination?.id)

        val root = fragment.requireView()
        root.findViewById<View>(R.id.btnAccurate).performClick()
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(root.findViewById<View>(R.id.cardAccuracy).visibility != View.VISIBLE)

        root.findViewById<View>(R.id.cardAccuracy).visibility = View.VISIBLE
        root.findViewById<View>(R.id.btnIncorrect).performClick()
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(root.findViewById<View>(R.id.cardAccuracy).visibility != View.VISIBLE)
    }

    private fun navFromHomeToQuestionnaireResult(activity: FragmentActivity): TestNavHostController {
        return TestNavHostController(activity).apply {
            setGraph(R.navigation.nav_graph)
            setCurrentDestination(R.id.nav_home)
            navigate(R.id.action_home_to_questionnaire)
            navigate(R.id.action_questionnaireFragment_to_questionnaireResultFragment)
        }
    }

    private fun callPrivate(target: Any, methodName: String, vararg args: Any?) {
        val method = target.javaClass.declaredMethods.first {
            it.name == methodName && it.parameterTypes.size == args.size
        }
        method.isAccessible = true
        method.invoke(target, *args)
    }
}
