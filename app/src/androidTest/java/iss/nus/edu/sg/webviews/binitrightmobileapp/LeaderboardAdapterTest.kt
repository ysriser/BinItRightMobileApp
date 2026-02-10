package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.graphics.Color
import android.widget.FrameLayout
import androidx.test.annotation.UiThreadTest
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.LeaderboardEntry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LeaderboardAdapterTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    @UiThreadTest
    fun testAdapter_Bind_TopRankings() {
        val items = listOf(
            LeaderboardEntry(1, "Alice", 100),
            LeaderboardEntry(2, "Bob", 90),
            LeaderboardEntry(3, "Charlie", 80),
            LeaderboardEntry(4, "David", 70)
        )

        val adapter = LeaderboardAdapter(items)
        val parent = FrameLayout(context)

        val holder1 = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder1, 0)

        assertEquals("1", holder1.tvRank.text.toString())
        assertEquals("Alice", holder1.tvUsername.text.toString())
        assertEquals("100", holder1.tvQuantity.text.toString())
        assertEquals(Color.parseColor("#FFD700"), holder1.tvRank.currentTextColor)

        val holder2 = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder2, 1)

        assertEquals("2", holder2.tvRank.text.toString())
        assertEquals("Bob", holder2.tvUsername.text.toString())
        assertEquals("90", holder2.tvQuantity.text.toString())
        assertEquals(Color.parseColor("#9E9E9E"), holder2.tvRank.currentTextColor)

        val holder3 = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder3, 2)

        assertEquals("3", holder3.tvRank.text.toString())
        assertEquals("Charlie", holder3.tvUsername.text.toString())
        assertEquals("80", holder3.tvQuantity.text.toString())
        assertEquals(Color.parseColor("#CD7F32"), holder3.tvRank.currentTextColor)

        val holder4 = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder4, 3)

        assertEquals("4", holder4.tvRank.text.toString())
        assertEquals("David", holder4.tvUsername.text.toString())
        assertEquals("70", holder4.tvQuantity.text.toString())
        assertEquals(Color.parseColor("#212121"), holder4.tvRank.currentTextColor)
    }

    @Test
    fun testAdapter_ItemCount() {
        val items = listOf(
            LeaderboardEntry(1, "User1", 10),
            LeaderboardEntry(2, "User2", 20)
        )
        val adapter = LeaderboardAdapter(items)
        assertEquals(2, adapter.itemCount)
    }
}