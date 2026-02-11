package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.content.Context
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.RecycleHistoryModel
import org.bouncycastle.crypto.params.Blake3Parameters.context
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RecycleHistoryAdapterTest {

    private lateinit var adapter: RecycleHistoryAdapter

    @Before
    fun setup() {
        adapter = RecycleHistoryAdapter()
    }

    @Test
    fun submitList_shouldUpdateItemCount() {
        val data = listOf(
            RecycleHistoryModel(
                categoryName = "Plastic",
                categoryIcon = "plastic",
                date = "2026-02-08",
                quantity = 2
            ),
            RecycleHistoryModel(
                categoryName = "Glass",
                categoryIcon = "glass",
                date = "2026-02-07",
                quantity = 1
            )
        )

        adapter.submitList(data)

        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun resolveIcon_plastic_returnsPlasticIcon() {
        val icon = adapter.resolveIcon("Plastic")
        assertEquals(R.drawable.ic_plastic, icon)
    }

    @Test
    fun resolveIcon_glass_returnsGlassIcon() {
        val icon = adapter.resolveIcon("Glass")
        assertEquals(R.drawable.ic_glass, icon)
    }

    @Test
    fun resolveIcon_unknown_returnsDefaultRecycleIcon() {
        val icon = adapter.resolveIcon("Metal")
        assertEquals(R.drawable.ic_recycle, icon)
    }
}