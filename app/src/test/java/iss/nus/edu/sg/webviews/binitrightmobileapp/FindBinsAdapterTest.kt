package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.app.Application
import android.content.Intent
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import iss.nus.edu.sg.todo.samplebin.FindBinsAdapter
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.DropOffLocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class FindBinsAdapterTest {
    private lateinit var adapter: FindBinsAdapter

    @Before
    fun setup() {
        adapter = FindBinsAdapter(emptyList())
    }

    @Test
    fun formatDistance_shouldConvertMetersToKm() {
        val result = adapter.formatDistance(1500.0)
        assertEquals("1.5 km away", result)
    }

    @Test
    fun mapBinType_blueBin_returnsGeneral() {
        val result = adapter.mapBinType("BLUEBIN")
        assertEquals("General", result)
    }

    @Test
    fun mapBinType_eWaste_returnsEWaste() {
        val result = adapter.mapBinType("EWASTE")
        assertEquals("E-Waste", result)
    }

    @Test
    fun mapBinType_lamp_returnsLighting() {
        val result = adapter.mapBinType("LAMP")
        assertEquals("Lighting", result)
    }

    @Test
    fun mapBinType_unknown_returnsOriginal() {
        val result = adapter.mapBinType("METAL")
        assertEquals("METAL", result)
    }

    @Test
    fun buildNavigationUri_stringFormat_isCorrect() {
        val lat = 1.3521
        val lng = 103.8198

        val uriString = adapter.buildNavigationUri(lat, lng).toString()

        assertEquals(
            "google.navigation:q=1.3521,103.8198",
            uriString
        )
    }

    @Test
    fun getItemCount_shouldReturnCorrectSize() {
        val bins = listOf(Any(), Any(), Any())
        val testAdapter = FindBinsAdapter(bins as List<Nothing>)

        assertEquals(3, testAdapter.itemCount)
    }

    @Test
    fun binTypeMapping_handlesKnownTypes() {
        assertEquals("General", adapter.mapBinType("BLUEBIN"))
        assertEquals("E-Waste", adapter.mapBinType("EWASTE"))
        assertEquals("Lighting", adapter.mapBinType("LIGHTING"))
    }

    @Test
    fun binTypeMapping_fallsBackForUnknown() {
        assertEquals("METAL", adapter.mapBinType("METAL"))
    }

    @Test
    fun onBindViewHolder_setsFieldsAndOpensDirectionsIntent() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val bin = DropOffLocation(
            id = "1",
            name = "Central Bin",
            address = "100 Main St",
            postalCode = "123456",
            description = "desc",
            binType = "BLUEBIN",
            latitude = 1.3521,
            longitude = 103.8198,
            status = true,
            distanceMeters = 1500.0
        )
        val testAdapter = FindBinsAdapter(listOf(bin))
        val holder = testAdapter.onCreateViewHolder(FrameLayout(activity), 0)

        testAdapter.onBindViewHolder(holder, 0)

        assertEquals("Central Bin", holder.itemView.findViewById<TextView>(R.id.txtName).text.toString())
        assertEquals("100 Main St", holder.itemView.findViewById<TextView>(R.id.txtAddress).text.toString())
        assertEquals("1.5 km away", holder.itemView.findViewById<TextView>(R.id.txtDistance).text.toString())
        assertEquals("General", holder.itemView.findViewById<TextView>(R.id.txtType).text.toString())

        holder.itemView.findViewById<android.widget.Button>(R.id.btnDirections).performClick()

        val intent = shadowOf(activity).nextStartedActivity
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals("com.google.android.apps.maps", intent.`package`)
        assertTrue(intent.dataString?.startsWith("google.navigation:q=1.3521,103.8198") == true)
    }
}
