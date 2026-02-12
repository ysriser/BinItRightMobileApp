package iss.nus.edu.sg.webviews.binitrightmobileapp


import iss.nus.edu.sg.webviews.binitrightmobileapp.model.RecycleHistoryModel
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
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
    fun resolveIcon_metal_returnsMetalIcon() {
        val icon = adapter.resolveIcon("Metal")
        assertEquals(R.drawable.ic_metal, icon)
    }

    @Test
    fun resolveIcon_unknown_returnsDefaultRecycleIcon() {
        val icon = adapter.resolveIcon("UnknownCategory")
        assertEquals(R.drawable.ic_recycle, icon)
    }

    @Test
    fun resolveIcon_lighting_returnsLightingIcon() {
        val icon = adapter.resolveIcon("Lighting")
        assertEquals(R.drawable.ic_lighting, icon)
    }

    @Test
    fun resolveIcon_others_returnsOthersIcon() {
        val icon = adapter.resolveIcon("Others")
        assertEquals(R.drawable.ic_others, icon)
    }
}
