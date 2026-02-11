package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.app.Application
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.DropOffLocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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

    @Test
    fun bindAndClick_selectsRow_thenSelectedButtonInvokesCallback() {
        val bins = listOf(mockBin("1").copy(name = ""), mockBin("2"))
        var selected: DropOffLocation? = null
        val adapter = NearByBinsAdapter(bins) { selected = it }
        val parent = FrameLayout(ApplicationProvider.getApplicationContext())

        val initialHolder = adapter.createViewHolder(parent, adapter.getItemViewType(0))
        adapter.bindViewHolder(initialHolder, 0)

        val nameView = initialHolder.itemView.findViewById<TextView>(R.id.binName)
        val distanceView = initialHolder.itemView.findViewById<TextView>(R.id.binDistance)
        assertEquals("Recycling Bin", nameView.text.toString())
        assertTrue(distanceView.text.toString().contains("m away"))

        initialHolder.itemView.performClick()

        val selectedHolder = adapter.createViewHolder(parent, 1)
        adapter.bindViewHolder(selectedHolder, 0)
        val button = selectedHolder.itemView.findViewById<Button>(R.id.selectButton)
        assertNotNull(button)
        button.performClick()

        assertEquals("1", selected?.id)
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
