package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.Accessory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class RewardShopAdapterTest {

    @Test
    fun ownedItem_setsOwnedAndDisablesButton() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val adapter = RewardShopAdapter(
            items = listOf(Accessory(1L, "Blue Cap", "img", 50, owned = true)),
            totalPoints = 999,
            onRedeemClick = {},
            onEquipClick = {}
        )

        val holder = adapter.onCreateViewHolder(FrameLayout(context), 0)
        adapter.onBindViewHolder(holder, 0)

        assertEquals("Owned", holder.btn.text.toString())
        assertFalse(holder.btn.isEnabled)
    }

    @Test
    fun redeemableItem_clickInvokesRedeemCallback() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        var clickedName: String? = null
        val adapter = RewardShopAdapter(
            items = listOf(Accessory(2L, "Green Hat", "img", 40, owned = false)),
            totalPoints = 100,
            onRedeemClick = { clickedName = it.name },
            onEquipClick = {}
        )

        val holder = adapter.onCreateViewHolder(FrameLayout(context), 0)
        adapter.onBindViewHolder(holder, 0)
        holder.btn.performClick()

        assertEquals("Redeem", holder.btn.text.toString())
        assertTrue(holder.btn.isEnabled)
        assertEquals("Green Hat", clickedName)
    }

    @Test
    fun insufficientPoints_setsNotEnoughAndDisablesButton() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val adapter = RewardShopAdapter(
            items = listOf(Accessory(3L, "Red Scarf", "img", 500, owned = false)),
            totalPoints = 20,
            onRedeemClick = {},
            onEquipClick = {}
        )

        val holder = adapter.onCreateViewHolder(FrameLayout(context), 0)
        adapter.onBindViewHolder(holder, 0)

        assertEquals("Not enough", holder.btn.text.toString())
        assertFalse(holder.btn.isEnabled)
    }
}