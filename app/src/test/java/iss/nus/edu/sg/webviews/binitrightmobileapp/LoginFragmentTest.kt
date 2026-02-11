package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.app.Application
import android.content.Context
import android.os.Looper
import android.view.View
import androidx.fragment.app.FragmentActivity
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.LoginResponse
import iss.nus.edu.sg.webviews.binitrightmobileapp.network.RetrofitClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
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
import org.robolectric.shadows.ShadowToast
import retrofit2.Response
import java.lang.reflect.Proxy
import java.util.Base64

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class LoginFragmentTest {

    @Test
    fun createAccountClick_navigatesToRegister() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        val fragment = LoginFragment()
        activity.supportFragmentManager.beginTransaction()
            .add(android.R.id.content, fragment)
            .commitNow()

        val navController = TestNavHostController(activity).apply {
            setGraph(R.navigation.nav_graph)
            setCurrentDestination(R.id.loginFragment)
        }
        Navigation.setViewNavController(fragment.requireView(), navController)

        fragment.requireView().findViewById<View>(R.id.tvCreateAccount).performClick()
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(R.id.registerFragment, navController.currentDestination?.id)
    }

    @Test
    fun handleLogin_withMissingFields_setsValidationErrors() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        val fragment = LoginFragment()
        activity.supportFragmentManager.beginTransaction()
            .add(android.R.id.content, fragment)
            .commitNow()

        fragment.requireView().findViewById<View>(R.id.btnSignIn).performClick()

        val usernameError = fragment.requireView()
            .findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.userUsername)
            .error
            ?.toString()
        val passwordError = fragment.requireView()
            .findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.userPassword)
            .error
            ?.toString()

        assertEquals("Username is required", usernameError)
        assertNull(passwordError)
    }

    @Test
    fun handleLogin_success_savesSessionAndNavigatesHome() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        activity.getSharedPreferences("APP_PREFS", Context.MODE_PRIVATE).edit().clear().apply()

        installApiServiceStub { methodName, _ ->
            when (methodName) {
                "login" -> Response.success(
                    LoginResponse(
                        success = true,
                        message = "Welcome",
                        token = buildJwt("101")
                    )
                )
                else -> throw UnsupportedOperationException(methodName)
            }
        }

        val fragment = LoginFragment()
        activity.supportFragmentManager.beginTransaction()
            .add(android.R.id.content, fragment)
            .commitNow()

        val navController = TestNavHostController(activity).apply {
            setGraph(R.navigation.nav_graph)
            setCurrentDestination(R.id.loginFragment)
        }
        Navigation.setViewNavController(fragment.requireView(), navController)

        fragment.requireView()
            .findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.appUsername)
            .setText("user_ok")
        fragment.requireView()
            .findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.appPassword)
            .setText("pass_ok")
        fragment.requireView().findViewById<View>(R.id.btnSignIn).performClick()

        waitMainUntil {
            navController.currentDestination?.id == R.id.nav_home
        }

        val prefs = activity.getSharedPreferences("APP_PREFS", Context.MODE_PRIVATE)
        assertNotNull(prefs.getString("TOKEN", null))
        assertEquals(101L, prefs.getLong("USER_ID", -1L))
        assertEquals(R.id.nav_home, navController.currentDestination?.id)
    }

    @Test
    fun handleLogin_unsuccessfulResponse_showsInvalidLoginToast() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        installApiServiceStub { methodName, _ ->
            when (methodName) {
                "login" -> Response.error<LoginResponse>(
                    401,
                    "nope".toResponseBody("text/plain".toMediaType())
                )
                else -> throw UnsupportedOperationException(methodName)
            }
        }

        val fragment = LoginFragment()
        activity.supportFragmentManager.beginTransaction()
            .add(android.R.id.content, fragment)
            .commitNow()

        fragment.requireView()
            .findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.appUsername)
            .setText("user_ok")
        fragment.requireView()
            .findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.appPassword)
            .setText("pass_ok")
        fragment.requireView().findViewById<View>(R.id.btnSignIn).performClick()
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals("Invalid login", ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun onDestroyView_clearsBinding() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        val fragment = LoginFragment()
        activity.supportFragmentManager.beginTransaction()
            .add(android.R.id.content, fragment)
            .commitNow()

        fragment.onDestroyView()
        assertNull(getPrivateField(fragment, "_binding"))
    }

    private fun buildJwt(sub: String): String {
        val encoder = Base64.getUrlEncoder().withoutPadding()
        val header = encoder.encodeToString("""{"alg":"HS256","typ":"JWT"}""".toByteArray())
        val payload = encoder.encodeToString("""{"sub":"$sub"}""".toByteArray())
        return "$header.$payload.sig"
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

    private fun getPrivateField(target: Any, fieldName: String): Any? {
        val field = target.javaClass.getDeclaredField(fieldName)
        field.isAccessible = true
        return field.get(target)
    }

    private fun waitMainUntil(loops: Int = 20, condition: () -> Boolean) {
        repeat(loops) {
            if (condition()) return
            shadowOf(Looper.getMainLooper()).idle()
        }
    }
}
