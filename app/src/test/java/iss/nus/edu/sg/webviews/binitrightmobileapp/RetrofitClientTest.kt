package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import iss.nus.edu.sg.webviews.binitrightmobileapp.network.RetrofitClient
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class RetrofitClientTest {

    @Test
    fun init_thenApiServiceIsAvailable() {
        val app = ApplicationProvider.getApplicationContext<Application>()

        RetrofitClient.init(app)

        assertNotNull(RetrofitClient.apiService())
    }

    @Test
    fun init_canBeCalledMultipleTimes() {
        val app = ApplicationProvider.getApplicationContext<Application>()

        RetrofitClient.init(app)
        RetrofitClient.init(app)

        assertNotNull(RetrofitClient.apiService())
    }
}
