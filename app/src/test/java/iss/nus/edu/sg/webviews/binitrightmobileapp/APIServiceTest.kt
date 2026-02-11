package iss.nus.edu.sg.webviews.binitrightmobileapp

import iss.nus.edu.sg.webviews.binitrightmobileapp.model.LoginRequest
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class APIServiceTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var api: ApiService

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        api = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    @Test
    fun login_returnsSuccess() = runTest {

        val mockResponse = MockResponse()
            .setResponseCode(200)
            .setBody("""{"success":true,"message":"OK","token":"abc"}""")

        mockWebServer.enqueue(mockResponse)

        val response = api.login(LoginRequest("u", "p"))

        assertTrue(response.isSuccessful)
        assertEquals("abc", response.body()?.token)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }
}