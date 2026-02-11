package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.view.ContextThemeWrapper
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.Accessory
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.UserAccessory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class AvatarAdapterTest {

    @Test
    fun equippedItem_disablesClickAndDimsView() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val themedContext = ContextThemeWrapper(context, R.style.Theme_BinItRightMobileApp)
        var clicked = false
        val accessory = Accessory(1L, "Blue Cap", "img", 10)
        val adapter = AvatarAdapter(
            items = listOf(UserAccessory(1L, equipped = true, accessories = accessory)),
            onItemClick = { clicked = true }
        )

        val holder = adapter.onCreateViewHolder(FrameLayout(themedContext), 0)
        adapter.onBindViewHolder(holder, 0)
        holder.binding.root.performClick()

        assertFalse(holder.binding.root.isClickable)
        assertEquals(0.7f, holder.binding.root.alpha)
        assertFalse(clicked)
    }

    @Test
    fun unequippedItem_clickInvokesCallback() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val themedContext = ContextThemeWrapper(context, R.style.Theme_BinItRightMobileApp)
        var clickedName: String? = null
        val accessory = Accessory(2L, "Green Hat", "img", 20)
        val adapter = AvatarAdapter(
            items = listOf(UserAccessory(2L, equipped = false, accessories = accessory)),
            onItemClick = { clickedName = it.accessories.name }
        )

        val holder = adapter.onCreateViewHolder(FrameLayout(themedContext), 0)
        adapter.onBindViewHolder(holder, 0)
        holder.binding.root.performClick()

        assertTrue(holder.binding.root.isClickable)
        assertEquals("Green Hat", clickedName)
    }
}