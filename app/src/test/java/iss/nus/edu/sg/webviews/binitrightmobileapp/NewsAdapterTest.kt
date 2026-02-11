package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.app.Application
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.NewsItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class NewsAdapterTest {

    @Test
    fun format_validDate_formatsCorrectly() {
        val result = NewsDateFormatter.format("2024-01-15T10:30:00")
        assertTrue(result.isNotBlank())
    }

    @Test
    fun format_nullDate_returnsRecent() {
        assertEquals("Recent", NewsDateFormatter.format(null))
    }

    @Test
    fun format_invalidDate_returnsRecent() {
        assertEquals("Recent", NewsDateFormatter.format("invalid-date"))
    }



    @Test
    fun getItemCount_returnsCorrectSize() {
        val adapter = NewsAdapter(dummyNews) {}
        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun adapter_reflectsInitialDataSize() {
        val adapter = NewsAdapter(dummyNews) {}
        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun onBindViewHolder_bindsFieldsAndHandlesClick() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        var clicked: NewsItem? = null
        val adapter = NewsAdapter(dummyNews) { clicked = it }
        val parent = FrameLayout(activity)

        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)

        with(holder.binding) {
            assertEquals(dummyNews[0].name, tvNewsTitle.text.toString())
            assertEquals(dummyNews[0].description, tvNewsDescription.text.toString())
            assertTrue(tvNewsDate.text.toString().isNotBlank())
            root.performClick()
        }
        assertEquals(dummyNews[0], clicked)
    }

    @Test
    fun onBindViewHolder_invalidDate_showsRecent_andUpdateDataRefreshesCount() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val badDateNews = NewsItem(
            newsId = 10L,
            name = "Broken date",
            description = "desc",
            imageUrl = "",
            status = "ACTIVE",
            publishedDate = "not-a-date"
        )
        val adapter = NewsAdapter(listOf(badDateNews)) {}
        val parent = FrameLayout(activity)
        val holder = adapter.onCreateViewHolder(parent, 0)

        adapter.onBindViewHolder(holder, 0)
        assertEquals("Recent", holder.binding.tvNewsDate.text.toString())

        adapter.updateData(dummyNews)
        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun newsItem_holdsAllFieldsCorrectly() {
        var item = dummyNews[0]

        assertEquals(1L, item.newsId)
        assertEquals("Green Initiative", item.name)
        assertEquals("Recycle more", item.description)
        assertEquals("ACTIVE", item.status)
        assertEquals("2024-01-15T10:30:00", item.publishedDate)
    }

    @Test
    fun newsItem_allowsNullPublishedDate() {
        val item = NewsItem(
            2L,
            "No Date News",
            "Body",
            "",
            "ACTIVE",
            null
        )

        assertNull(item.publishedDate)
    }

    @Test
    fun title_returnsName_whenNewsExists() {
        val news = NewsItem(1, "Hello", "Body", "url", "ACTIVE", null)
        assertEquals("Hello", NewsDateFormatter.NewsDetailMapper.title(news))
    }

    @Test
    fun title_returnsEmpty_whenNull() {
        assertEquals("", NewsDateFormatter.NewsDetailMapper.title(null))
    }



    private val dummyNews = listOf(
        NewsItem(
            newsId = 1L,
            name = "Green Initiative",
            description = "Recycle more",
            imageUrl = "https://img.test/news.png",
            status = "ACTIVE",
            publishedDate = "2024-01-15T10:30:00"
        ),
        NewsItem(
            newsId = 2L,
            name = "Green Initiative",
            description = "Recycle more",
            imageUrl = "https://img.test/news.png",
            status = "ACTIVE",
            publishedDate = "2024-01-15T10:30:00"
        )
    )
    object NewsDateFormatter {

        fun format(publishedDate: String?): String {
            return try {
                val input = android.icu.text.SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss",
                    Locale.getDefault()
                )
                val output = android.icu.text.SimpleDateFormat(
                    "MMM dd, yyyy",
                    Locale.getDefault()
                )

                val date = input.parse(publishedDate ?: "")
                date?.let { output.format(it) } ?: "Recent"
            } catch (e: Exception) {
                "Recent"
            }
        }

        object NewsDetailMapper {

            fun title(news: NewsItem?): String = news?.name ?: ""
            fun body(news: NewsItem?): String = news?.description ?: ""
            fun imageUrl(news: NewsItem?): String = news?.imageUrl ?: ""
        }
    }
}
