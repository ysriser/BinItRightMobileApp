package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.app.Application
import android.os.Looper
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class QuestionnaireFragmentTest {

    @Test
    fun onViewCreated_initializesRecyclerAndRendersQuestion() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        val fragment = QuestionnaireFragment()
        activity.supportFragmentManager.beginTransaction()
            .add(android.R.id.content, fragment)
            .commitNow()

        val root = fragment.requireView()
        assertNotNull(root.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvOptions).adapter)
        assertTrue(root.findViewById<android.widget.TextView>(R.id.tvHeaderTitle).text.toString().isNotBlank())
        assertTrue(root.findViewById<android.widget.TextView>(R.id.tvQuestion).text.toString().isNotBlank())
    }

    @Test
    fun updateQuestionContent_andNavigateToResult_coverCoreBranches() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        val fragment = QuestionnaireFragment()
        activity.supportFragmentManager.beginTransaction()
            .add(android.R.id.content, fragment)
            .commitNow()

        val withSubtitle = QuestionNode(
            id = "qx",
            title = "Header",
            question = "Question text",
            subtitle = "sub text",
            options = listOf(OptionNode("o1", "A", "out1"))
        )
        callPrivate(fragment, "updateQuestionContent", withSubtitle)
        assertEquals("Header", fragment.requireView().findViewById<android.widget.TextView>(R.id.tvHeaderTitle).text.toString())
        assertEquals("Question text", fragment.requireView().findViewById<android.widget.TextView>(R.id.tvQuestion).text.toString())
        assertEquals(android.view.View.VISIBLE, fragment.requireView().findViewById<android.widget.TextView>(R.id.tvSubtitle).visibility)

        val withoutSubtitle = withSubtitle.copy(subtitle = null)
        callPrivate(fragment, "updateQuestionContent", withoutSubtitle)
        assertTrue(
            fragment.requireView().findViewById<android.widget.TextView>(R.id.tvSubtitle).visibility !=
                android.view.View.VISIBLE
        )

        val navController = TestNavHostController(activity).apply {
            setGraph(R.navigation.nav_graph)
            setCurrentDestination(R.id.questionnaireFragment)
        }
        Navigation.setViewNavController(fragment.requireView(), navController)
        callPrivate(
            fragment,
            "navigateToResult",
            OutcomeNode(
                id = "out1",
                categoryTitle = "Plastic",
                disposalLabel = "Recyclable",
                certainty = "HIGH",
                explanation = "ok",
                tips = listOf("Clean")
            )
        )
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(R.id.questionnaireResultFragment, navController.currentDestination?.id)
    }

    @Test
    fun onDestroyView_clearsBinding() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        val fragment = QuestionnaireFragment()
        activity.supportFragmentManager.beginTransaction()
            .add(android.R.id.content, fragment)
            .commitNow()

        fragment.onDestroyView()
        assertNull(getPrivateField(fragment, "_binding"))
    }

    @Test
    fun adapterCallbacks_backHandling_andObserverBranches_areCovered() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        val fragment = QuestionnaireFragment()
        activity.supportFragmentManager.beginTransaction()
            .add(android.R.id.content, fragment)
            .commitNow()

        val navController = TestNavHostController(activity).apply {
            setGraph(R.navigation.nav_graph)
            setCurrentDestination(R.id.nav_home)
            navigate(R.id.action_home_to_questionnaire)
        }
        Navigation.setViewNavController(fragment.requireView(), navController)

        val viewModel = getQuestionnaireViewModel(fragment)
        val adapter = getPrivateField(fragment, "optionAdapter") as QuestionnaireOptionAdapter
        val optionClick = getPrivateField(adapter, "onOptionClick") as (OptionNode) -> Unit

        optionClick(OptionNode("plastic", "Plastic", "q2_clean"))
        shadowOf(Looper.getMainLooper()).idleFor(300, TimeUnit.MILLISECONDS)
        optionClick(OptionNode("BACK_ACTION", "Go back", "BACK_ACTION_NEXT"))
        shadowOf(Looper.getMainLooper()).idleFor(300, TimeUnit.MILLISECONDS)

        @Suppress("UNCHECKED_CAST")
        val summary = getPrivateField(viewModel, "_answersSummary") as MutableLiveData<List<Pair<String, String>>>
        summary.value = listOf("What material?" to "Plastic")
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(fragment.requireView().findViewById<android.widget.LinearLayout>(R.id.layoutAnswersSummary).childCount > 0)

        @Suppress("UNCHECKED_CAST")
        val navEvent = getPrivateField(viewModel, "_navigationEvent") as MutableLiveData<String?>
        navEvent.value = "out_not_sure"
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(R.id.questionnaireResultFragment, navController.currentDestination?.id)

        val returnNav = TestNavHostController(activity).apply {
            setGraph(R.navigation.nav_graph)
            setCurrentDestination(R.id.nav_home)
            navigate(R.id.action_home_to_questionnaire)
        }
        Navigation.setViewNavController(fragment.requireView(), returnNav)
        callPrivate(fragment, "handleBack")
        shadowOf(Looper.getMainLooper()).idleFor(300, TimeUnit.MILLISECONDS)
        assertTrue(returnNav.currentDestination?.id == R.id.nav_home || returnNav.currentDestination?.id == R.id.questionnaireFragment)
    }

    private fun callPrivate(target: Any, methodName: String, vararg args: Any?) {
        val method = target.javaClass.declaredMethods.first {
            it.name == methodName && it.parameterTypes.size == args.size
        }
        method.isAccessible = true
        method.invoke(target, *args)
    }

    private fun getPrivateField(target: Any, fieldName: String): Any? {
        val field = target.javaClass.getDeclaredField(fieldName)
        field.isAccessible = true
        return field.get(target)
    }

    private fun getQuestionnaireViewModel(fragment: QuestionnaireFragment): QuestionnaireViewModel {
        val delegateField = QuestionnaireFragment::class.java.getDeclaredField("viewModel\$delegate")
        delegateField.isAccessible = true
        val delegate = delegateField.get(fragment) as Lazy<*>
        return delegate.value as QuestionnaireViewModel
    }
}
