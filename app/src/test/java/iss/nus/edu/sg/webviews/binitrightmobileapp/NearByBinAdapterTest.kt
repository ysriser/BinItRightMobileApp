package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.app.Application
import androidx.recyclerview.widget.RecyclerView
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.DropOffLocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [28],
    application = Application::class
)
class NearByBinAdapterTest {
    //Adapter return count
    @Test
    fun getItemCount_returnsCorrectSize() {
        val bins = listOf(
            mockBin("1"),
            mockBin("2"),
            mockBin("3")
        )

        val adapter = NearByBinsAdapter(bins) {}

        assertEquals(3, adapter.itemCount)
    }

    @Test
    fun getItemViewType_default_isNormal() {
        val adapter = NearByBinsAdapter(listOf(mockBin("1"))) {}

        val viewType = adapter.getItemViewType(0)

        assertEquals(0, viewType)
    }

    @Test
    fun select_updatesSelectedPosition() {
        val state = BinSelectionState()

        state.select(1)

        assertTrue(state.isSelected(1))
    }

    @Test
    fun default_isNormalViewType() {
        val state = BinSelectionState()
        assertEquals(0, state.getItemViewType(0))
    }

    @Test
    fun select_changesViewTypeToSelected() {
        val state = BinSelectionState()
        state.select(0)

        assertEquals(1, state.getItemViewType(0))
    }

    private fun mockBin(id: String) = DropOffLocation(
        id = id,
        name = "Bin $id",
        address = "Address $id",
        postalCode = "123456",
        description = "Test bin",
        binType = "BLUEBIN",
        latitude = 1.3,
        longitude = 103.8,
        status = true,
        distanceMeters = 50.0
    )

    class BinSelectionState {
        var selectedPosition = RecyclerView.NO_POSITION

        fun getItemViewType(position: Int): Int =
            if (position == selectedPosition) 1 else 0
        fun isSelected(position: Int): Boolean =
            position == selectedPosition

        fun select(position: Int) {
            selectedPosition = position
        }
    }

}
