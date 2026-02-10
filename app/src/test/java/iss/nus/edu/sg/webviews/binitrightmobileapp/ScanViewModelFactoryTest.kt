package iss.nus.edu.sg.webviews.binitrightmobileapp

import androidx.lifecycle.ViewModel
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ScanViewModelFactoryTest {

    private class DummyViewModel : ViewModel()

    @Test
    fun create_scanViewModel_returnsExpectedType() {
        val context = RuntimeEnvironment.getApplication()
        val factory = ScanViewModelFactory(context)

        val vm: ViewModel = factory.create(ScanViewModel::class.java)

        assertEquals(ScanViewModel::class.java.name, vm::class.java.name)
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_unknownViewModel_throwsIllegalArgument() {
        val context = RuntimeEnvironment.getApplication()
        val factory = ScanViewModelFactory(context)

        factory.create(DummyViewModel::class.java)
    }
}
