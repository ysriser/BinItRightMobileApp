package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.graphics.Color
import android.os.Looper
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class QuestionnaireOptionAdapterTest {

    @Test
    fun diffCallback_comparesByIdAndContent() {
        val diff = QuestionnaireOptionAdapter.OptionDiffCallback()
        val oldOption = OptionNode(id = "1", text = "Yes", next = "q2")
        val sameIdNewText = OptionNode(id = "1", text = "No", next = "q2")
        val same = OptionNode(id = "1", text = "Yes", next = "q2")

        assertTrue(diff.areItemsTheSame(oldOption, sameIdNewText))
        assertTrue(diff.areContentsTheSame(oldOption, same))
    }

    @Test
    fun backAction_optionUsesSpecialStyleAndTriggersClick() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        var clickedId: String? = null
        val adapter = QuestionnaireOptionAdapter { clickedId = it.id }
        val option = OptionNode(id = "BACK_ACTION", text = "Back", next = "BACK_ACTION")

        adapter.submitList(listOf(option))
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        val holder = adapter.onCreateViewHolder(FrameLayout(context), 0)
        adapter.onBindViewHolder(holder, 0)
        holder.itemView.performClick()

        assertEquals(Color.parseColor("#546E7A"), holder.itemView.findViewById<android.widget.TextView>(R.id.tvOptionText).currentTextColor)
        assertEquals("BACK_ACTION", clickedId)
    }
}