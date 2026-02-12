package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.app.Application
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.os.Environment
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.FragmentActivity
import iss.nus.edu.sg.webviews.binitrightmobileapp.databinding.FragmentCheckInBinding
import iss.nus.edu.sg.webviews.binitrightmobileapp.network.RetrofitClient
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
import org.robolectric.shadows.ShadowMediaMetadataRetriever
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response
import java.lang.reflect.Proxy
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class CheckInFragmentTest {

    @Test
    fun onViewCreated_readsArguments_andUpdatesLocationViews() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).create().get()
        val fragment = CheckInFragment().apply {
            arguments = Bundle().apply {
                putString("binId", "b-1")
                putString("binName", "Green Bin")
                putString("binAddress", "100 Street")
                putString("binType", "BLUEBIN")
                putDouble("binLatitude", 1.300)
                putDouble("binLongitude", 103.800)
                putString("wasteCategory", "plastic bottle")
            }
        }

        activity.supportFragmentManager.beginTransaction()
            .add(android.R.id.content, fragment)
            .commitNow()

        val binding = FragmentCheckInBinding.inflate(LayoutInflater.from(activity))
        setPrivateField(fragment, "_binding", binding)
        callPrivate(fragment, "retrieveBinInformation")
        callPrivate(fragment, "displayBinInformation")
        val name = binding.tvLocationName.text.toString()
        val address = binding.tvLocationAddress.text.toString()

        assertEquals("Green Bin", name)
        assertEquals("100 Street", address)
    }

    @Test
    fun updateCounterDisplay_andValidation_withoutVideo_setsErrorStatus() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).create().get()
        val fragment = CheckInFragment()
        activity.supportFragmentManager.beginTransaction()
            .add(android.R.id.content, fragment)
            .commitNow()

        val binding = FragmentCheckInBinding.inflate(LayoutInflater.from(activity))
        setPrivateField(fragment, "_binding", binding)
        setPrivateField(fragment, "binId", "b-1")
        setPrivateField(fragment, "wasteType", "Plastic")
        setPrivateField(fragment, "currentCount", 12)
        fragment.updateCounterDisplay()
        callPrivate(fragment, "handleSubmitWithValidation")

        val countText = binding.tvItemCount.text.toString()
        val statusText = binding.tvStatusMessage.text.toString()

        assertEquals("12", countText)
        assertTrue(statusText.contains("Video required"))
    }

    @Test
    fun validation_quantityNotHigh_withoutVideoDuration_showsInvalidDuration() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).create().get()
        val fragment = CheckInFragment()
        activity.supportFragmentManager.beginTransaction()
            .add(android.R.id.content, fragment)
            .commitNow()

        val binding = FragmentCheckInBinding.inflate(LayoutInflater.from(activity))
        setPrivateField(fragment, "_binding", binding)
        setPrivateField(fragment, "binId", "b-1")
        setPrivateField(fragment, "wasteType", "Plastic")
        setPrivateField(fragment, "currentCount", 1)
        setPrivateField(fragment, "recordedFile", null)
        callPrivate(fragment, "handleSubmitWithValidation")

        val statusText = binding.tvStatusMessage.text.toString()
        assertTrue(statusText.contains("Invalid check-in"))
    }

    @Test
    fun checkLocationAndNavigate_withoutPermission_returnsEarly() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).create().get()
        val fragment = CheckInFragment()
        activity.supportFragmentManager.beginTransaction()
            .add(android.R.id.content, fragment)
            .commitNow()

        fragment.checkLocationAndNavigate()
        assertTrue(fragment.isAdded)
    }

    @Test
    fun preselectChip_andChipListener_updateWasteTypeAndButtonStates() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).create().get()
        val fragment = CheckInFragment()
        activity.supportFragmentManager.beginTransaction()
            .add(android.R.id.content, fragment)
            .commitNow()

        val binding = FragmentCheckInBinding.inflate(LayoutInflater.from(activity))
        setPrivateField(fragment, "_binding", binding)

        setPrivateField(fragment, "mappedWasteCategory", "Metal")
        setPrivateField(fragment, "wasteCategory", "plastic bottle")
        setPrivateField(fragment, "selectedBinType", "EWASTE")
        setPrivateField(fragment, "binType", "EWASTE")
        callPrivate(fragment, "preselectWasteTypeChip")

        assertEquals(R.id.chipMetal, binding.cgItemType.checkedChipId)
        assertEquals("Metal", getPrivateField(fragment, "wasteType"))

        callPrivate(fragment, "setupChipListeners")
        binding.cgItemType.check(R.id.chipLighting)
        assertEquals("Lighting", getPrivateField(fragment, "wasteType"))

        callPrivate(fragment, "updateSubmitButtonState", true)
        assertTrue(binding.btnSubmit.isEnabled)
        assertEquals(1.0f, binding.btnSubmit.alpha)

        callPrivate(fragment, "updateSubmitButtonState", false)
        assertTrue(!binding.btnSubmit.isEnabled)
        assertEquals(0.5f, binding.btnSubmit.alpha)

        callPrivate(fragment, "disableRecordVideo")
        assertTrue(!binding.btnRecordVideo.isEnabled)
        assertEquals("VIDEO SUBMITTED", binding.btnRecordVideo.text.toString())

        callPrivate(fragment, "enableRecordVideo")
        assertTrue(binding.btnRecordVideo.isEnabled)
        assertEquals("RECORD VIDEO", binding.btnRecordVideo.text.toString())

        callPrivate(fragment, "showStatus", "ok-msg", false)
        assertEquals("ok-msg", binding.tvStatusMessage.text.toString())
        assertEquals(View.VISIBLE, binding.tvStatusMessage.visibility)
    }

    @Test
    fun retrieveBinInfo_loadLastVideo_andDestroyView_coverAdditionalBranches() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).create().get()
        val fragment = CheckInFragment().apply {
            arguments = Bundle().apply {
                putString("binId", "b2")
                putString("binName", "")
                putString("binAddress", "")
                putString("selectedBinType", "EWASTE")
                putString("wasteCategory", "unknown object")
                putString("mappedWasteCategory", "")
            }
        }
        activity.supportFragmentManager.beginTransaction()
            .add(android.R.id.content, fragment)
            .commitNow()

        val binding = FragmentCheckInBinding.inflate(LayoutInflater.from(activity))
        setPrivateField(fragment, "_binding", binding)

        callPrivate(fragment, "retrieveBinInformation")
        callPrivate(fragment, "displayBinInformation")
        callPrivate(fragment, "preselectWasteTypeChip")

        assertEquals("Recycling Bin", binding.tvLocationName.text.toString())
        assertEquals("Location not available", binding.tvLocationAddress.text.toString())
        assertEquals(R.id.chipEWaste, binding.cgItemType.checkedChipId)
        assertEquals("E-Waste", getPrivateField(fragment, "wasteType"))

        val videoDir = activity.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        assertNotNull(videoDir)
        val testVideo = java.io.File(videoDir, "unit_video.mp4")
        testVideo.writeBytes(byteArrayOf(1, 2, 3))

        setPrivateField(fragment, "isSubmitted", false)
        callPrivate(fragment, "loadLastRecordedVideo")
        assertNotNull(getPrivateField(fragment, "recordedFile"))
        assertTrue(binding.btnSubmit.isEnabled)

        fragment.onDestroyView()
        assertNull(getPrivateField(fragment, "_binding"))
    }

    @Test
    fun preselectWasteTypeChip_supportsMultipleMappedTypes() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).create().get()
        val fragment = CheckInFragment()
        activity.supportFragmentManager.beginTransaction()
            .add(android.R.id.content, fragment)
            .commitNow()

        val binding = FragmentCheckInBinding.inflate(LayoutInflater.from(activity))
        setPrivateField(fragment, "_binding", binding)

        val cases = listOf(
            "Plastic" to R.id.chipPlastic,
            "Paper" to R.id.chipPaper,
            "Glass" to R.id.chipGlass,
            "Lighting" to R.id.chipLighting,
            "Others" to R.id.chipOthers
        )

        cases.forEach { (mapped, chipId) ->
            setPrivateField(fragment, "mappedWasteCategory", mapped)
            callPrivate(fragment, "preselectWasteTypeChip")
            assertEquals(chipId, binding.cgItemType.checkedChipId)
        }
    }

    @Test
    fun onResume_whenSubmitted_disablesRecordAndSubmit() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).create().get()
        val fragment = CheckInFragment()
        activity.supportFragmentManager.beginTransaction()
            .add(android.R.id.content, fragment)
            .commitNow()

        val binding = FragmentCheckInBinding.inflate(LayoutInflater.from(activity))
        setPrivateField(fragment, "_binding", binding)
        setPrivateField(fragment, "isSubmitted", true)

        fragment.onResume()

        assertTrue(!binding.btnRecordVideo.isEnabled)
        assertEquals("VIDEO SUBMITTED", binding.btnRecordVideo.text.toString())
        assertTrue(!binding.btnSubmit.isEnabled)
        assertEquals(0.5f, binding.btnSubmit.alpha)
    }

    @Test
    fun submitWithoutVideo_successResponse_setsSubmittedTrue() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).create().get()
        installApiServiceStub { methodName ->
            if (methodName == "submitRecycleCheckIn") {
                Response.success(CheckInDataResponse("SUCCESS", "ok"))
            } else {
                throw UnsupportedOperationException(methodName)
            }
        }

        val fragment = CheckInFragment()
        activity.supportFragmentManager.beginTransaction()
            .add(android.R.id.content, fragment)
            .commitNow()
        val binding = FragmentCheckInBinding.inflate(LayoutInflater.from(activity))
        setPrivateField(fragment, "_binding", binding)
        setPrivateField(fragment, "binId", "b-3")
        setPrivateField(fragment, "wasteType", "Plastic")
        setPrivateField(fragment, "currentCount", 2)

        callPrivate(fragment, "submitWithoutVideo", 6)
        shadowOf(Looper.getMainLooper()).idle()
        shadowOf(Looper.getMainLooper()).idleFor(3, TimeUnit.SECONDS)

        assertEquals(true, getPrivateField(fragment, "isSubmitted"))
    }

    @Test
    fun submitWithoutVideo_failureResponses_updateStatusAndButtons() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).create().get()

        val fragment = CheckInFragment()
        activity.supportFragmentManager.beginTransaction()
            .add(android.R.id.content, fragment)
            .commitNow()
        val binding = FragmentCheckInBinding.inflate(LayoutInflater.from(activity))
        setPrivateField(fragment, "_binding", binding)
        setPrivateField(fragment, "binId", "b-4")
        setPrivateField(fragment, "wasteType", "Paper")
        setPrivateField(fragment, "currentCount", 1)

        installApiServiceStub { methodName ->
            if (methodName == "submitRecycleCheckIn") {
                Response.success(CheckInDataResponse("FAIL", "Rejected"))
            } else {
                throw UnsupportedOperationException(methodName)
            }
        }
        callPrivate(fragment, "submitWithoutVideo", 7)
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(binding.tvStatusMessage.text.toString().contains("FAIL"))
        assertTrue(binding.btnSubmit.isEnabled)
        assertTrue(binding.btnRecordVideo.isEnabled)

        installApiServiceStub { methodName ->
            if (methodName == "submitRecycleCheckIn") {
                Response.error<CheckInDataResponse>(
                    500,
                    "server-error".toResponseBody("text/plain".toMediaType())
                )
            } else {
                throw UnsupportedOperationException(methodName)
            }
        }
        callPrivate(fragment, "submitWithoutVideo", 7)
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(binding.tvStatusMessage.text.toString().contains("Submission failed (500)"))
        assertTrue(binding.btnSubmit.isEnabled)
    }

    @Test
    fun submitWithoutVideo_nullBodyAndThrownException_coverRemainingFailureBranches() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).create().get()
        val fragment = CheckInFragment()
        activity.supportFragmentManager.beginTransaction()
            .add(android.R.id.content, fragment)
            .commitNow()
        val binding = FragmentCheckInBinding.inflate(LayoutInflater.from(activity))
        setPrivateField(fragment, "_binding", binding)
        setPrivateField(fragment, "binId", "b-5")
        setPrivateField(fragment, "wasteType", "Glass")
        setPrivateField(fragment, "currentCount", 1)

        installApiServiceStub { methodName ->
            if (methodName == "submitRecycleCheckIn") {
                @Suppress("UNCHECKED_CAST")
                Response.success<CheckInDataResponse?>(null) as Response<CheckInDataResponse>
            } else {
                throw UnsupportedOperationException(methodName)
            }
        }
        callPrivate(fragment, "submitWithoutVideo", 8)
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(binding.tvStatusMessage.text.toString().contains("Empty server response"))

        installApiServiceStub { methodName ->
            if (methodName == "submitRecycleCheckIn") {
                throw IllegalStateException("boom-submit")
            } else {
                throw UnsupportedOperationException(methodName)
            }
        }
        callPrivate(fragment, "submitWithoutVideo", 8)
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(binding.tvStatusMessage.text.toString().contains("Submission failed: boom-submit"))
        assertTrue(binding.btnSubmit.isEnabled)
        assertTrue(binding.btnRecordVideo.isEnabled)
    }

    @Test
    fun handleSubmitWithValidation_quantityHigh_routesToUpload_andCoversPresignFailures() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).create().get()
        val fragment = CheckInFragment()
        activity.supportFragmentManager.beginTransaction()
            .add(android.R.id.content, fragment)
            .commitNow()
        val binding = FragmentCheckInBinding.inflate(LayoutInflater.from(activity))
        setPrivateField(fragment, "_binding", binding)
        setPrivateField(fragment, "currentCount", 11)
        setPrivateField(fragment, "binId", "b-6")
        setPrivateField(fragment, "wasteType", "Metal")

        val video = java.io.File(activity.getExternalFilesDir(Environment.DIRECTORY_MOVIES), "submit-video.mp4")
        video.parentFile?.mkdirs()
        video.writeBytes(byteArrayOf(0, 1, 2))
        ShadowMediaMetadataRetriever.addMetadata(
            video.absolutePath,
            MediaMetadataRetriever.METADATA_KEY_DURATION,
            "8000"
        )
        setPrivateField(fragment, "recordedFile", video)

        installApiServiceStub { methodName ->
            if (methodName == "getPresignedUpload") {
                Response.error<PresignUploadResponse>(
                    401,
                    "unauthorized".toResponseBody("text/plain".toMediaType())
                )
            } else {
                throw UnsupportedOperationException(methodName)
            }
        }
        callPrivate(fragment, "handleSubmitWithValidation")
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(binding.tvStatusMessage.text.toString().contains("Failed to get upload permission"))
        assertTrue(binding.btnSubmit.isEnabled)
        assertTrue(binding.btnRecordVideo.isEnabled)

        installApiServiceStub { methodName ->
            if (methodName == "getPresignedUpload") {
                throw IllegalStateException("presign-boom")
            } else {
                throw UnsupportedOperationException(methodName)
            }
        }
        callPrivate(fragment, "handleSubmitWithValidation")
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(binding.tvStatusMessage.text.toString().contains("Error: presign-boom"))
        assertTrue(binding.btnSubmit.isEnabled)
        assertTrue(binding.btnRecordVideo.isEnabled)
    }

    private fun callPrivate(target: Any, methodName: String, vararg args: Any?) {
        val method = target.javaClass.declaredMethods.first {
            it.name == methodName && it.parameterTypes.size == args.size
        }
        method.isAccessible = true
        method.invoke(target, *args)
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

    private fun installApiServiceStub(handler: (methodName: String) -> Any?) {
        val proxy = Proxy.newProxyInstance(
            ApiService::class.java.classLoader,
            arrayOf(ApiService::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "toString" -> "ApiServiceStub"
                "hashCode" -> 0
                "equals" -> false
                else -> handler(method.name)
            }
        } as ApiService

        val apiField = RetrofitClient::class.java.getDeclaredField("api")
        apiField.isAccessible = true
        apiField.set(RetrofitClient, proxy)
    }
}
