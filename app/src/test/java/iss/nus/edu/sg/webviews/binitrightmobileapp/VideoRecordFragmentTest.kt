package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.Manifest
import android.app.Application
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.camera.video.Recording
import iss.nus.edu.sg.webviews.binitrightmobileapp.databinding.FragmentVideoRecordBinding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class VideoRecordFragmentTest {

    @Test
    fun hasPermissions_reflectsRuntimePermissionState() {
        val activity = Robolectric.buildActivity(AppCompatActivity::class.java).setup().get()
        val fragment = VideoRecordFragment()
        attachWithoutInflatingLayout(activity, fragment)

        shadowOf(activity).denyPermissions(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        assertFalse(callPrivate(fragment, "hasPermissions") as Boolean)

        shadowOf(activity).grantPermissions(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        assertTrue(callPrivate(fragment, "hasPermissions") as Boolean)
    }

    @Test
    fun startRecording_whenAlreadyRecording_stopsAndResetsUi() {
        val activity = Robolectric.buildActivity(AppCompatActivity::class.java).setup().get()
        val fragment = VideoRecordFragment()
        attachWithoutInflatingLayout(activity, fragment)
        val binding = FragmentVideoRecordBinding.inflate(LayoutInflater.from(activity))
        setPrivateField(fragment, "_binding", binding)

        val activeRecording: Recording = mock()
        setPrivateField(fragment, "recording", activeRecording)
        callPrivate(fragment, "startRecording")

        verify(activeRecording).stop()
        assertNull(getPrivateField(fragment, "recording"))
        assertEquals(
            activity.getString(R.string.video_action_start_recording),
            binding.btnStartRecording.text.toString()
        )
        assertEquals("Recording stopped", ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun onDestroyView_withRecording_stopsAndClearsState() {
        val activity = Robolectric.buildActivity(AppCompatActivity::class.java).setup().get()
        val fragment = VideoRecordFragment()
        attachWithoutInflatingLayout(activity, fragment)
        val binding = FragmentVideoRecordBinding.inflate(LayoutInflater.from(activity))
        setPrivateField(fragment, "_binding", binding)

        val activeRecording: Recording = mock()
        setPrivateField(fragment, "recording", activeRecording)

        fragment.onDestroyView()

        verify(activeRecording).stop()
        assertNull(getPrivateField(fragment, "_binding"))
        assertNull(getPrivateField(fragment, "recording"))
    }

    private fun callPrivate(target: Any, methodName: String, vararg args: Any?): Any? {
        val method = target.javaClass.declaredMethods.first {
            it.name == methodName && it.parameterTypes.size == args.size
        }
        method.isAccessible = true
        return method.invoke(target, *args)
    }

    private fun setPrivateField(target: Any, fieldName: String, value: Any?) {
        val field = target.javaClass.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(target, value)
    }

    private fun getPrivateField(target: Any, fieldName: String): Any? {
        val field = target.javaClass.getDeclaredField(fieldName)
        field.isAccessible = true
        return field.get(target)
    }

    private fun attachWithoutInflatingLayout(activity: AppCompatActivity, fragment: Fragment) {
        val layoutField = Fragment::class.java.getDeclaredField("mContentLayoutId")
        layoutField.isAccessible = true
        layoutField.setInt(fragment, 0)
        activity.supportFragmentManager.beginTransaction().add(fragment, "video").commitNow()
    }
}
