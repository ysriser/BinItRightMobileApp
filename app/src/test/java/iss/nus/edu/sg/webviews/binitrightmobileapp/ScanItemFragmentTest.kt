package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.Manifest
import android.app.Application
import android.net.Uri
import android.os.Looper
import android.view.View
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class ScanItemFragmentTest {

    @Test
    fun closeClick_andTakePhotoWithoutCamera_coverSafeBranches() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        shadowOf(activity).denyPermissions(Manifest.permission.CAMERA)

        val fragment = ScanItemFragment()
        activity.supportFragmentManager.beginTransaction()
            .add(android.R.id.content, fragment)
            .commitNow()

        val navController = TestNavHostController(activity).apply {
            setGraph(R.navigation.nav_graph)
            setCurrentDestination(R.id.nav_home)
            navigate(R.id.action_home_to_scanItem)
        }
        Navigation.setViewNavController(fragment.requireView(), navController)

        fragment.requireView().findViewById<View>(R.id.btnClose).performClick()
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(R.id.nav_home, navController.currentDestination?.id)

        callPrivate(fragment, "takePhoto")
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals("Camera is not ready yet", ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun scanUiHelpers_andLifecycle_coverAdditionalLines() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        shadowOf(activity).denyPermissions(Manifest.permission.CAMERA)

        val fragment = ScanItemFragment()
        activity.supportFragmentManager.beginTransaction()
            .add(android.R.id.content, fragment)
            .commitNow()

        callPrivate(fragment, "startScanUI")
        assertTrue(fragment.requireView().findViewById<View>(R.id.loadingOverlay).visibility == View.VISIBLE)

        callPrivate(fragment, "stopScanUI")
        assertTrue(fragment.requireView().findViewById<View>(R.id.loadingOverlay).visibility != View.VISIBLE)

        val output = callPrivate(fragment, "getOutputDirectory") as File
        assertTrue(output.exists())

        val granted = callPrivate(fragment, "allPermissionsGranted") as Boolean
        assertFalse(granted)

        fragment.onResume()
        assertTrue(fragment.requireView().findViewById<View>(R.id.btnTakePhoto).isEnabled)

        fragment.onDestroyView()
        assertNull(getPrivateField(fragment, "_binding"))
    }

    @Test
    fun observers_handleStatusSuccessAndFailureBranches() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        shadowOf(activity).denyPermissions(Manifest.permission.CAMERA)

        val fragment = ScanItemFragment()
        activity.supportFragmentManager.beginTransaction()
            .add(android.R.id.content, fragment)
            .commitNow()

        val navController = TestNavHostController(activity).apply {
            setGraph(R.navigation.nav_graph)
            setCurrentDestination(R.id.nav_home)
            navigate(R.id.action_home_to_scanItem)
        }
        Navigation.setViewNavController(fragment.requireView(), navController)

        val viewModel = getScanViewModel(fragment)
        @Suppress("UNCHECKED_CAST")
        val scanningStatus = getPrivateField(viewModel, "_scanningStatus") as MutableLiveData<String>
        @Suppress("UNCHECKED_CAST")
        val scanResult = getPrivateField(viewModel, "_scanResult") as MutableLiveData<Result<ScanResult>?>

        scanningStatus.value = "Analyzing..."
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(
            "Analyzing...",
            fragment.requireView().findViewById<android.widget.TextView>(R.id.tvScanProgress).text.toString()
        )

        callPrivate(fragment, "startScanUI")
        setPrivateField(fragment, "lastCapturedUri", Uri.parse("file:///tmp/capture.jpg"))
        fragment.requireView().findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.switchDebug).isChecked = true
        scanResult.value = Result.success(
            ScanResult(
                category = "Plastic",
                recyclable = true,
                confidence = 0.92f,
                debugMessage = "provider: openai"
            )
        )
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(R.id.scanningResultFragment, navController.currentDestination?.id)

        val retryNav = TestNavHostController(activity).apply {
            setGraph(R.navigation.nav_graph)
            setCurrentDestination(R.id.scanItemFragment)
        }
        Navigation.setViewNavController(fragment.requireView(), retryNav)
        callPrivate(fragment, "startScanUI")
        fragment.requireView().findViewById<View>(R.id.btnTakePhoto).isEnabled = false
        scanResult.value = Result.failure(Exception("scan boom"))
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(fragment.requireView().findViewById<View>(R.id.btnTakePhoto).isEnabled)
        assertTrue(fragment.requireView().findViewById<View>(R.id.loadingOverlay).visibility != View.VISIBLE)
    }

    private fun callPrivate(target: Any, methodName: String, vararg args: Any?): Any? {
        val method = target.javaClass.declaredMethods.first {
            it.name == methodName && it.parameterTypes.size == args.size
        }
        method.isAccessible = true
        return method.invoke(target, *args)
    }

    private fun getPrivateField(target: Any, fieldName: String): Any? {
        val field = target.javaClass.getDeclaredField(fieldName)
        field.isAccessible = true
        return field.get(target)
    }

    private fun setPrivateField(target: Any, fieldName: String, value: Any?) {
        val field = target.javaClass.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(target, value)
    }

    private fun getScanViewModel(fragment: ScanItemFragment): ScanViewModel {
        val delegateField = ScanItemFragment::class.java.getDeclaredField("viewModel\$delegate")
        delegateField.isAccessible = true
        val delegate = delegateField.get(fragment) as Lazy<*>
        return delegate.value as ScanViewModel
    }
}
