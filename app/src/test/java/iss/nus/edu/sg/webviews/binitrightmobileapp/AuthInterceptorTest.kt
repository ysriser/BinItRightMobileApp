package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.app.Application
import android.content.Context
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.AuthInterceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class AuthInterceptorTest {

    private lateinit var context: Application
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        clearToken()
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        clearToken()
        server.shutdown()
    }

    @Test
    fun intercept_addsAuthorizationHeader_whenTokenExists() {
        saveToken("abc123")
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(context))
            .build()

        val request = Request.Builder()
            .url(server.url("/scan"))
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            assertEquals(200, response.code)
        }

        val recorded = server.takeRequest()
        assertEquals("Bearer abc123", recorded.getHeader("Authorization"))
    }

    @Test
    fun intercept_skipsAuthorizationHeader_whenTokenMissing() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(context))
            .build()

        val request = Request.Builder()
            .url(server.url("/scan"))
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            assertEquals(200, response.code)
        }

        val recorded = server.takeRequest()
        assertNull(recorded.getHeader("Authorization"))
    }

    @Test
    fun intercept_on401_clearsToken_andBroadcastsAuthFailure() {
        saveToken("expired-token")
        server.enqueue(MockResponse().setResponseCode(401).setBody("{}"))

        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(context))
            .build()

        val request = Request.Builder()
            .url(server.url("/secure"))
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            assertEquals(401, response.code)
        }

        val prefs = context.getSharedPreferences("APP_PREFS", Context.MODE_PRIVATE)
        assertNull(prefs.getString("TOKEN", null))

        val broadcasts = shadowOf(context).broadcastIntents
        assertFalse(broadcasts.isEmpty())
        assertEquals(AuthInterceptor.AUTH_FAILED_ACTION, broadcasts.last().action)
    }

    private fun saveToken(token: String) {
        context.getSharedPreferences("APP_PREFS", Context.MODE_PRIVATE)
            .edit()
            .putString("TOKEN", token)
            .commit()
    }

    private fun clearToken() {
        context.getSharedPreferences("APP_PREFS", Context.MODE_PRIVATE)
            .edit()
            .remove("TOKEN")
            .commit()
    }
}