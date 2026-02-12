package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.app.Application
import android.content.Context
import android.os.Looper
import android.view.View
import android.widget.Spinner
import androidx.fragment.app.FragmentActivity
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.IssueCreateRequest
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.IssueResponse
import iss.nus.edu.sg.webviews.binitrightmobileapp.network.RetrofitClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast
import retrofit2.Response
import java.lang.reflect.Proxy

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class ReportIssueDialogFragmentTest {

    @Test
    fun onViewCreated_setsSpinnerCategories() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        val dialog = ReportIssueDialogFragment()
        dialog.show(activity.supportFragmentManager, "issue")
        shadowOf(Looper.getMainLooper()).idle()

        val spinner = dialog.requireView().findViewById<Spinner>(R.id.spinnerCategory)
        assertEquals(4, spinner.adapter.count)
    }

    @Test
    fun submitIssue_emptyDescription_setsValidationError() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        val dialog = ReportIssueDialogFragment()
        dialog.show(activity.supportFragmentManager, "issue")
        shadowOf(Looper.getMainLooper()).idle()

        dialog.requireView().findViewById<View>(R.id.btnSubmit).performClick()
        val error = dialog.requireView()
            .findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilDescription)
            .error
            ?.toString()

        assertEquals("Please describe the issue", error)
    }

    @Test
    fun submitIssue_withoutUserId_showsToastAndDismisses() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        activity.getSharedPreferences("APP_PREFS", Context.MODE_PRIVATE)
            .edit()
            .remove("USER_ID")
            .apply()

        val dialog = ReportIssueDialogFragment()
        dialog.show(activity.supportFragmentManager, "issue")
        shadowOf(Looper.getMainLooper()).idle()

        dialog.requireView()
            .findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etDescription)
            .setText("desc")
        dialog.requireView().findViewById<View>(R.id.btnSubmit).performClick()
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals("User not logged in", ShadowToast.getTextOfLatestToast())
        assertFalse(dialog.isAdded)
    }

    @Test
    fun submitIssue_success_submitsMappedCategoryAndDismisses() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        activity.getSharedPreferences("APP_PREFS", Context.MODE_PRIVATE)
            .edit()
            .putLong("USER_ID", 77L)
            .apply()

        var capturedRequest: IssueCreateRequest? = null
        installApiServiceStub { methodName, args ->
            when (methodName) {
                "createIssue" -> {
                    capturedRequest = args?.firstOrNull() as? IssueCreateRequest
                    Response.success(IssueResponse(issueId = 55L))
                }
                else -> throw UnsupportedOperationException(methodName)
            }
        }

        val dialog = ReportIssueDialogFragment()
        dialog.show(activity.supportFragmentManager, "issue")
        shadowOf(Looper.getMainLooper()).idle()

        dialog.requireView().findViewById<Spinner>(R.id.spinnerCategory).setSelection(1)
        dialog.requireView()
            .findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etDescription)
            .setText("network issue")
        dialog.requireView().findViewById<View>(R.id.btnSubmit).performClick()

        waitMainUntil {
            !dialog.isAdded
        }

        assertEquals("APP_PROBLEMS", capturedRequest?.issueCategory)
        assertEquals("network issue", capturedRequest?.description)
        assertEquals(true, ShadowToast.getTextOfLatestToast().toString().contains("Issue reported successfully!"))
    }

    @Test
    fun submitIssue_failure_showsHttpCodeMessageAndKeepsDialog() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        activity.getSharedPreferences("APP_PREFS", Context.MODE_PRIVATE)
            .edit()
            .putLong("USER_ID", 77L)
            .apply()

        installApiServiceStub { methodName, _ ->
            when (methodName) {
                "createIssue" -> Response.error<IssueResponse>(
                    500,
                    "server error".toResponseBody("text/plain".toMediaType())
                )
                else -> throw UnsupportedOperationException(methodName)
            }
        }

        val dialog = ReportIssueDialogFragment()
        dialog.show(activity.supportFragmentManager, "issue")
        shadowOf(Looper.getMainLooper()).idle()

        dialog.requireView()
            .findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etDescription)
            .setText("network issue")
        dialog.requireView().findViewById<View>(R.id.btnSubmit).performClick()
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals("Failed to submit issue: 500", ShadowToast.getTextOfLatestToast())
        assertTrue(dialog.isAdded)
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
