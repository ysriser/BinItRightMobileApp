package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.graphics.Color
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.LeaderboardEntry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class LeaderboardAdapterTest {

    @Test
    fun onBind_setsRankTextAndTopThreeColor() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val adapter = LeaderboardAdapter(
            listOf(
                LeaderboardEntry(1L, "alice", 50),
                LeaderboardEntry(2L, "bob", 40),
                LeaderboardEntry(3L, "carl", 30),
                LeaderboardEntry(4L, "dina", 20)
            )
        )

        val parent = FrameLayout(context)
        val firstHolder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(firstHolder, 0)
        assertEquals("1", firstHolder.tvRank.text.toString())
        assertEquals(Color.parseColor("#FFD700"), firstHolder.tvRank.currentTextColor)

        val fourthHolder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(fourthHolder, 3)
        assertEquals("4", fourthHolder.tvRank.text.toString())
        assertEquals(Color.parseColor("#212121"), fourthHolder.tvRank.currentTextColor)
    }

    @Test
    fun itemCount_matchesInputSize() {
        val adapter = LeaderboardAdapter(listOf(LeaderboardEntry(10L, "eve", 5)))
        assertEquals(1, adapter.itemCount)
    }
}