package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.test.annotation.UiThreadTest
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import iss.nus.edu.sg.webviews.binitrightmobileapp.databinding.ItemAchievementBinding
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.Achievement
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AchievementAdapterTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun testDiffCallback() {
        val callback = AchievementAdapter.AchievementDiffCallback()
        val item1 = Achievement(1L, "Name A", "Desc", "url", "crit", true)
        val item2 = Achievement(1L, "Name A", "Desc", "url", "crit", true) // ID 内容都一样
        val item3 = Achievement(2L, "Name B", "Desc", "url", "crit", true) // ID 不同
        val item4 = Achievement(1L, "Name Changed", "Desc", "url", "crit", true) // ID 一样，内容不同

        assertTrue(callback.areItemsTheSame(item1, item2))
        assertFalse(callback.areItemsTheSame(item1, item3))

        assertTrue(callback.areContentsTheSame(item1, item2))
        assertFalse(callback.areContentsTheSame(item1, item4))
    }

    @Test
    @UiThreadTest
    fun testViewHolder_Bind_UnlockedState() {
        val parent = FrameLayout(context)
        val binding = ItemAchievementBinding.inflate(LayoutInflater.from(context), parent, false)
        val viewHolder = AchievementAdapter.AchievementViewHolder(binding)

        val item = Achievement(1L, "Master Recycler", "Recycle 100 items", "url", "crit", true)

        viewHolder.bind(item)

        assertEquals("Master Recycler", binding.tvName.text.toString())
        assertEquals("Recycle 100 items", binding.tvDescription.text.toString())

        assertEquals(1.0f, binding.root.alpha, 0.01f) // 透明度 1.0
        assertEquals(View.VISIBLE, binding.tvTapToView.visibility) // 提示文字可见
        assertNull(binding.ivBadge.colorFilter)
        assertNotNull(binding.ivStatusIcon.drawable)
    }

    @Test
    @UiThreadTest
    fun testViewHolder_Bind_LockedState() {
        val parent = FrameLayout(context)
        val binding = ItemAchievementBinding.inflate(LayoutInflater.from(context), parent, false)
        val viewHolder = AchievementAdapter.AchievementViewHolder(binding)

        val item = Achievement(2L, "Rookie", "Recycle 1 item", "url", "crit", false)

        viewHolder.bind(item)

        assertEquals("Rookie", binding.tvName.text.toString())

        assertEquals(0.6f, binding.root.alpha, 0.01f) // 半透明
        assertEquals(View.GONE, binding.tvTapToView.visibility) // 提示文字隐藏
        assertNotNull(binding.ivBadge.colorFilter)
    }

    @Test
    @UiThreadTest
    fun testAdapter_ClickEvent() {
        var clickedItem: Achievement? = null

        val adapter = TestAchievementAdapter { clickedItem = it }
        val item = Achievement(10L, "Click Me", "Desc", "url", "crit", true)

        adapter.setTestList(listOf(item))

        val parent = FrameLayout(context)
        val viewHolder = adapter.onCreateViewHolder(parent, 0)

        adapter.onBindViewHolder(viewHolder, 0)

        viewHolder.itemView.performClick()

        assertEquals(item, clickedItem)
    }
    class TestAchievementAdapter(onClick: (Achievement) -> Unit) : AchievementAdapter(onClick) {
        private var testList: List<Achievement> = emptyList()

        fun setTestList(list: List<Achievement>) {
            this.testList = list
            submitList(list)
        }

        public override fun getItem(position: Int): Achievement {
            return testList[position]
        }
    }
}