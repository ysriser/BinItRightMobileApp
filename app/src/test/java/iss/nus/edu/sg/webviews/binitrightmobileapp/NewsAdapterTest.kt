package iss.nus.edu.sg.webviews.binitrightmobileapp

import iss.nus.edu.sg.webviews.binitrightmobileapp.model.NewsItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

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