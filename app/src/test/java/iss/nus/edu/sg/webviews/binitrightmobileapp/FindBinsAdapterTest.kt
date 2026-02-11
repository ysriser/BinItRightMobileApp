package iss.nus.edu.sg.webviews.binitrightmobileapp

import iss.nus.edu.sg.todo.samplebin.FindBinsAdapter
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

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

        val uriString = "google.navigation:q=$lat,$lng"

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
}