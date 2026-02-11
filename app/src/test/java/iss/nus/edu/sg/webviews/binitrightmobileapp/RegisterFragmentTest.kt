package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.app.Application
import android.os.Looper
import android.view.View
import androidx.fragment.app.FragmentActivity
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.RegisterResponse
import iss.nus.edu.sg.webviews.binitrightmobileapp.network.RetrofitClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
class RegisterFragmentTest {

    @Test
    fun validation_branches_showExpectedErrors() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        val fragment = RegisterFragment()
        activity.supportFragmentManager.beginTransaction()
            .add(android.R.id.content, fragment)
            .commitNow()

        val root = fragment.requireView()
        root.findViewById<View>(R.id.btnCreateAccount).performClick()
        assertEquals(
            "Username required",
            root.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilUsername).error?.toString()
        )

        root.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etUsername).setText("ok_user")
        root.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etPassword).setText("123")
        root.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etConfirmPassword).setText("123")
        root.findViewById<View>(R.id.btnCreateAccount).performClick()
        assertEquals(
            "Min 6 characters",
            root.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilPassword).error?.toString()
        )

        root.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etPassword).setText("123456")
        root.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etConfirmPassword).setText("654321")
        root.findViewById<View>(R.id.btnCreateAccount).performClick()
        assertEquals(
            "Passwords do not match",
            root.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilConfirmPassword).error?.toString()
        )
    }

    @Test
    fun backToLoginClick_navigatesToLogin() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        val fragment = RegisterFragment()
        activity.supportFragmentManager.beginTransaction()
            .add(android.R.id.content, fragment)
            .commitNow()

        val navController = TestNavHostController(activity).apply {
            setGraph(R.navigation.nav_graph)
            setCurrentDestination(R.id.registerFragment)
        }
        Navigation.setViewNavController(fragment.requireView(), navController)

        fragment.requireView().findViewById<View>(R.id.tvBackToLogin).performClick()
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(R.id.loginFragment, navController.currentDestination?.id)
        assertEquals("Going to login", ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun createAccount_success_navigatesToLogin() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        installApiServiceStub { methodName, _ ->
            when (methodName) {
                "register" -> Response.success(RegisterResponse(true, "created"))
                else -> throw UnsupportedOperationException(methodName)
            }
        }

        val fragment = RegisterFragment()
        activity.supportFragmentManager.beginTransaction()
            .add(android.R.id.content, fragment)
            .commitNow()

        val navController = TestNavHostController(activity).apply {
            setGraph(R.navigation.nav_graph)
            setCurrentDestination(R.id.registerFragment)
        }
        Navigation.setViewNavController(fragment.requireView(), navController)

        fragment.requireView().findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etUsername)
            .setText("ok_user")
        fragment.requireView().findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etPassword)
            .setText("123456")
        fragment.requireView().findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etConfirmPassword)
            .setText("123456")
        fragment.requireView().findViewById<View>(R.id.btnCreateAccount).performClick()

        waitMainUntil {
            navController.currentDestination?.id == R.id.loginFragment
        }

        assertEquals(R.id.loginFragment, navController.currentDestination?.id)
    }

    @Test
    fun createAccount_failure_showsServerErrorMessage() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        installApiServiceStub { methodName, _ ->
            when (methodName) {
                "register" -> Response.error<RegisterResponse>(
                    400,
                    "Registration failed".toResponseBody("text/plain".toMediaType())
                )
                else -> throw UnsupportedOperationException(methodName)
            }
        }

        val fragment = RegisterFragment()
        activity.supportFragmentManager.beginTransaction()
            .add(android.R.id.content, fragment)
            .commitNow()

        fragment.requireView().findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etUsername)
            .setText("ok_user")
        fragment.requireView().findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etPassword)
            .setText("123456")
        fragment.requireView().findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etConfirmPassword)
            .setText("123456")
        fragment.requireView().findViewById<View>(R.id.btnCreateAccount).performClick()
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals("Registration failed", ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun createAccount_exception_showsErrorToast() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        installApiServiceStub { methodName, _ ->
            when (methodName) {
                "register" -> throw IllegalStateException("network down")
                else -> throw UnsupportedOperationException(methodName)
            }
        }

        val fragment = RegisterFragment()
        activity.supportFragmentManager.beginTransaction()
            .add(android.R.id.content, fragment)
            .commitNow()

        fragment.requireView().findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etUsername)
            .setText("ok_user")
        fragment.requireView().findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etPassword)
            .setText("123456")
        fragment.requireView().findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etConfirmPassword)
            .setText("123456")
        fragment.requireView().findViewById<View>(R.id.btnCreateAccount).performClick()
        shadowOf(Looper.getMainLooper()).idle()

        val latestToast = ShadowToast.getTextOfLatestToast().toString()
        assertEquals(true, latestToast.startsWith("Error:"))
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
