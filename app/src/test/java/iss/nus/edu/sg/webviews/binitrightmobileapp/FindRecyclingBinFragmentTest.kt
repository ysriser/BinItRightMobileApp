package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.app.Application
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.UiSettings
import iss.nus.edu.sg.todo.samplebin.FindBinsAdapter
import iss.nus.edu.sg.webviews.binitrightmobileapp.databinding.FragmentFindRecyclingBinBinding
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.DropOffLocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.Mockito.mockStatic
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class FindRecyclingBinFragmentTest {

    @Test
    fun parseAllBinsJson_whenFragmentAdded_updatesLists_andHandlesBadItem() {
        val activity = Robolectric.buildActivity(AppCompatActivity::class.java).setup().get()
        val fragment = FindRecyclingBinFragment()
        attachWithoutInflatingLayout(activity, fragment)

        val binding = FragmentFindRecyclingBinBinding.inflate(LayoutInflater.from(activity))
        setPrivateField(fragment, "_binding", binding)
        @Suppress("UNCHECKED_CAST")
        val filtered = getPrivateField(fragment, "filteredBins") as MutableList<DropOffLocation>
        setPrivateField(fragment, "adapter", FindBinsAdapter(filtered))
        setPrivateField(fragment, "isMapReady", false)

        val mixedJson = """
            [
              {"id":"a1","name":"Bin A","address":"Addr A","postalCode":"123456","description":"d","binType":"BLUEBIN","status":"ACTIVE","latitude":1.30,"longitude":103.80,"distanceMeters":100.0},
              "bad-item",
              {"id":"a2","name":"Bin B","address":"Addr B","postalCode":"654321","description":"d","binType":"EWASTE","status":"INACTIVE","latitude":1.31,"longitude":103.81,"distanceMeters":200.0}
            ]
        """.trimIndent()

        callPrivate(fragment, "parseAllBinsJson", mixedJson)
        callPrivate(fragment, "parseAllBinsJson", "not-json")

        @Suppress("UNCHECKED_CAST")
        val all = getPrivateField(fragment, "allBins") as MutableList<DropOffLocation>
        assertEquals(2, all.size)
        assertEquals(2, filtered.size)

        @Suppress("UNCHECKED_CAST")
        val pending = getPrivateField(fragment, "pendingBins") as? List<DropOffLocation>
        assertTrue(pending == null || pending.isNotEmpty())
    }

    @Test
    fun setupChipFiltering_clicksCoverFilterTriggers() {
        val activity = Robolectric.buildActivity(AppCompatActivity::class.java).setup().get()
        val fragment = FindRecyclingBinFragment()
        attachWithoutInflatingLayout(activity, fragment)
        val binding = FragmentFindRecyclingBinBinding.inflate(LayoutInflater.from(activity))
        setPrivateField(fragment, "_binding", binding)
        callPrivate(fragment, "setupChipFiltering")
        shadowOf(activity).denyPermissions(android.Manifest.permission.ACCESS_FINE_LOCATION)

        binding.chipAll.performClick()
        assertTrue(binding.chipAll.isChecked)
    }

    @Test
    fun setupRecyclerView_applyFilter_updateUi_andDestroyView_coverUiBranches() {
        val activity = Robolectric.buildActivity(AppCompatActivity::class.java).setup().get()
        val fragment = FindRecyclingBinFragment()
        attachWithoutInflatingLayout(activity, fragment)
        val binding = FragmentFindRecyclingBinBinding.inflate(LayoutInflater.from(activity))
        setPrivateField(fragment, "_binding", binding)
        callPrivate(fragment, "setupRecyclerView")

        @Suppress("UNCHECKED_CAST")
        val all = getPrivateField(fragment, "allBins") as MutableList<DropOffLocation>
        @Suppress("UNCHECKED_CAST")
        val filtered = getPrivateField(fragment, "filteredBins") as MutableList<DropOffLocation>
        all.clear()
        filtered.clear()
        all.addAll(listOf(bin("1", "BLUEBIN"), bin("2", "EWASTE"), bin("3", "EWASTE")))
        setPrivateField(fragment, "isMapReady", false)

        callPrivate(fragment, "applyFilter", "EWASTE")
        assertEquals(2, filtered.size)
        callPrivate(fragment, "applyFilter", "ALL")
        assertEquals(3, filtered.size)

        val tokenBefore = getPrivateField(fragment, "lastCameraMoveToken") as Int
        callPrivate(fragment, "updateUI")
        val tokenAfter = getPrivateField(fragment, "lastCameraMoveToken") as Int
        assertEquals(3, filtered.size)
        assertTrue(tokenAfter > tokenBefore)

        callPrivate(fragment, "moveCamera", emptyList<DropOffLocation>(), tokenAfter)

        fragment.onDestroyView()
        assertNotNull(binding.binsRecyclerView.adapter)
        assertEquals(null, getPrivateField(fragment, "_binding"))
    }

    @Test
    fun onMapReady_withPendingBins_coversMapReadyPathAndClearsQueue() {
        val fragment = FindRecyclingBinFragment()
        val map: GoogleMap = mock()
        val uiSettings: UiSettings = mock()
        whenever(map.uiSettings).thenReturn(uiSettings)
        val zoomUpdate: CameraUpdate = mock()

        setPrivateField(fragment, "hasFetchedInitialBins", true)
        setPrivateField(fragment, "pendingBins", listOf(bin("10", "BLUEBIN"), bin("11", "EWASTE")))

        mockStatic(CameraUpdateFactory::class.java).use { factory ->
            factory.`when`<CameraUpdate> { CameraUpdateFactory.newLatLngZoom(any(), any()) }
                .thenReturn(zoomUpdate)
            fragment.onMapReady(map)
        }

        assertEquals(true, getPrivateField(fragment, "isMapReady"))
        assertEquals(null, getPrivateField(fragment, "pendingBins"))
        verify(uiSettings).isZoomControlsEnabled = true
        verify(map).clear()
        verify(map, times(2)).addMarker(any())
        verify(map).animateCamera(any())
    }

    @Test
    fun updateMapMarkers_mapReady_limitsTo40_andMoveCameraHonoursToken() {
        val fragment = FindRecyclingBinFragment()
        val map: GoogleMap = mock()
        val uiSettings: UiSettings = mock()
        whenever(map.uiSettings).thenReturn(uiSettings)
        val zoomUpdate: CameraUpdate = mock()
        val boundsUpdate: CameraUpdate = mock()

        doAnswer { invocation ->
            val callback = invocation.getArgument<GoogleMap.OnMapLoadedCallback>(0)
            callback.onMapLoaded()
            null
        }.whenever(map).setOnMapLoadedCallback(any())

        setPrivateField(fragment, "googleMap", map)
        setPrivateField(fragment, "isMapReady", true)

        val manyBins = (1..50).map { idx ->
            DropOffLocation(
                id = "$idx",
                name = "Bin $idx",
                address = "Addr $idx",
                postalCode = "123456",
                description = "desc",
                binType = if (idx % 2 == 0) "BLUEBIN" else "EWASTE",
                latitude = 1.3 + idx * 0.0001,
                longitude = 103.8 + idx * 0.0001,
                status = true,
                distanceMeters = idx.toDouble()
            )
        }

        mockStatic(CameraUpdateFactory::class.java).use { factory ->
            factory.`when`<CameraUpdate> { CameraUpdateFactory.newLatLngZoom(any(), any()) }
                .thenReturn(zoomUpdate)
            factory.`when`<CameraUpdate> { CameraUpdateFactory.newLatLngBounds(any(), any()) }
                .thenReturn(boundsUpdate)

            callPrivate(fragment, "updateMapMarkers", manyBins, 1)
            verify(map).clear()
            verify(map, times(40)).addMarker(any())
            verify(map, times(1)).animateCamera(any())

            setPrivateField(fragment, "lastCameraMoveToken", 10)
            callPrivate(fragment, "moveCamera", manyBins.take(2), 9)
            verify(map, times(0)).moveCamera(any())

            callPrivate(fragment, "moveCamera", manyBins.take(2), 10)
            verify(map, times(1)).moveCamera(any())
        }
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
        activity.supportFragmentManager.beginTransaction().add(fragment, "find-bins").commitNow()
    }
}
