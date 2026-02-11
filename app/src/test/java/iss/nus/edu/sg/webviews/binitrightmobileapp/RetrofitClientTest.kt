package iss.nus.edu.sg.webviews.binitrightmobileapp

import iss.nus.edu.sg.webviews.binitrightmobileapp.network.RetrofitClient
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class RetrofitClientTest {

    @Test(expected = UninitializedPropertyAccessException::class)
    fun apiService_beforeInit_throwsException() {
        RetrofitClient.apiService()
    }

    @Test
    fun init_setsApiSuccessfully() {
        val context = RuntimeEnvironment.getApplication()

        RetrofitClient.init(context)

        val api = RetrofitClient.apiService()

        assertNotNull(api)
    }
}