package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.app.Application
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import iss.nus.edu.sg.webviews.binitrightmobileapp.databinding.FragmentNearByBinBinding
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.DropOffLocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class NearByBinFragmentTest {

    @Test
    fun retrieveScanArguments_andUpdateFlowHint_coverMappings() {
        val activity = Robolectric.buildActivity(AppCompatActivity::class.java).setup().get()
        val fragment = NearByBinFragment().apply {
            arguments = android.os.Bundle().apply {
                putString("selectedBinType", "ewaste")
                putString("wasteCategory", "E-waste - Charger")
            }
        }
        attachWithoutInflatingLayout(activity, fragment)
        val binding = FragmentNearByBinBinding.inflate(LayoutInflater.from(activity))
        setPrivateField(fragment, "_binding", binding)

        callPrivate(fragment, "retrieveScanArguments")
        callPrivate(fragment, "updateFlowHint")

        assertEquals("EWASTE", getPrivateField(fragment, "selectedBinType"))
        assertEquals("E-Waste", getPrivateField(fragment, "mappedWasteCategory"))
        assertTrue(binding.tvFlowHint.text.toString().contains("e-waste", ignoreCase = true))

        setPrivateField(fragment, "selectedBinType", "BLUEBIN")
        callPrivate(fragment, "updateFlowHint")
        assertTrue(binding.tvFlowHint.text.toString().contains("blue"))
    }

    @Test
    fun setupRecycler_applyFilter_andPendingMapQueue_coverCoreBranches() {
        val activity = Robolectric.buildActivity(AppCompatActivity::class.java).setup().get()
        val fragment = NearByBinFragment()
        attachWithoutInflatingLayout(activity, fragment)
        val binding = FragmentNearByBinBinding.inflate(LayoutInflater.from(activity))
        setPrivateField(fragment, "_binding", binding)

        callPrivate(fragment, "setupRecyclerView")
        @Suppress("UNCHECKED_CAST")
        val nearby = getPrivateField(fragment, "nearbyBins") as MutableList<DropOffLocation>
        nearby.clear()
        nearby.addAll(listOf(bin("1", "BLUEBIN"), bin("2", "EWASTE")))
        setPrivateField(fragment, "isMapReady", false)

        callPrivate(fragment, "applyFlowFilter")

        @Suppress("UNCHECKED_CAST")
        val displayed = getPrivateField(fragment, "displayedBins") as MutableList<DropOffLocation>
        assertEquals(2, displayed.size)
        assertNotNull(getPrivateField(fragment, "pendingBins"))
        assertNotNull(binding.binsRecyclerView.adapter)

        callPrivate(fragment, "updateMapMarkers", emptyList<DropOffLocation>())
        assertNotNull(getPrivateField(fragment, "pendingBins"))
    }

    @Test
    fun applyFlowFilter_withSelectedBinType_keepsOnlyMatchingBins() {
        val activity = Robolectric.buildActivity(AppCompatActivity::class.java).setup().get()
        val fragment = NearByBinFragment()
        attachWithoutInflatingLayout(activity, fragment)
        val binding = FragmentNearByBinBinding.inflate(LayoutInflater.from(activity))
        setPrivateField(fragment, "_binding", binding)

        callPrivate(fragment, "setupRecyclerView")
        setPrivateField(fragment, "selectedBinType", "EWASTE")

        @Suppress("UNCHECKED_CAST")
        val nearby = getPrivateField(fragment, "nearbyBins") as MutableList<DropOffLocation>
        nearby.clear()
        nearby.addAll(listOf(bin("1", "BLUEBIN"), bin("2", "EWASTE"), bin("3", "LIGHTING")))
        setPrivateField(fragment, "isMapReady", false)

        callPrivate(fragment, "applyFlowFilter")

        @Suppress("UNCHECKED_CAST")
        val displayed = getPrivateField(fragment, "displayedBins") as MutableList<DropOffLocation>
        assertEquals(1, displayed.size)
        assertEquals("EWASTE", displayed.first().binType)
    }

    @Test
    fun onDestroyView_clearsBinding() {
        val activity = Robolectric.buildActivity(AppCompatActivity::class.java).setup().get()
        val fragment = NearByBinFragment()
        attachWithoutInflatingLayout(activity, fragment)
        val binding = FragmentNearByBinBinding.inflate(LayoutInflater.from(activity))
        setPrivateField(fragment, "_binding", binding)

        fragment.onDestroyView()
        assertNull(getPrivateField(fragment, "_binding"))
    }

    private fun bin(id: String, type: String) = DropOffLocation(
        id = id,
        name = "Bin $id",
        address = "Address $id",
        postalCode = "123456",
        description = "desc",
        binType = type,
        latitude = 1.3,
        longitude = 103.8,
        status = true,
        distanceMeters = 12.0
    )

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

    private fun attachWithoutInflatingLayout(activity: AppCompatActivity, fragment: Fragment) {
        val layoutField = Fragment::class.java.getDeclaredField("mContentLayoutId")
        layoutField.isAccessible = true
        layoutField.setInt(fragment, 0)
        activity.supportFragmentManager.beginTransaction().add(fragment, "nearby").commitNow()
    }
}
