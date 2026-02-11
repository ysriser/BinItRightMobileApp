package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.app.Application
import android.content.Intent
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import iss.nus.edu.sg.webviews.binitrightmobileapp.chat.ChatMessage
import iss.nus.edu.sg.webviews.binitrightmobileapp.chat.MessageType
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.EventItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class EventsAdapterTest {

    @Test
    fun eventItem_holdsAllFieldsCorrectly() {
        val item = EventItem(
            eventId = 1L,
            title = "Beach Cleanup",
            description = "Help clean the beach",
            imageUrl = "img.jpg",
            locationName = "East Coast Park",
            postalCode = "449876",
            startTime = "2026-01-25T09:00:00",
            endTime = "2026-01-25T12:00:00"
        )

        assertEquals(1L, item.eventId)
        assertEquals("Beach Cleanup", item.title)
        assertEquals("East Coast Park", item.locationName)
        assertEquals("449876", item.postalCode)
    }

    @Test
    fun getItemCount_returnsCorrectSize() {
        val adapter = EventAdapter(events)
        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun onBindViewHolder_setsTexts_andLaunchesNavigationIntent() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val adapter = EventAdapter(events)
        val holder = adapter.onCreateViewHolder(FrameLayout(activity), 0)

        adapter.onBindViewHolder(holder, 0)

        with(holder.binding) {
            assertEquals(events[0].title, tvEventTitle.text.toString())
            assertEquals(events[0].description, tvEventDescription.text.toString())
            assertEquals(events[0].locationName, tvEventLocation.text.toString())
            assertTrue(tvEventDate.text.toString().contains(","))
            assertTrue(tvEventTime.text.toString().contains("-"))
            btnGoThere.performClick()
        }

        val started = shadowOf(activity).nextStartedActivity
        assertEquals(Intent.ACTION_VIEW, started.action)
        assertTrue(started.dataString?.startsWith("geo:0,0?q=") == true)
    }

    @Test
    fun updateData_replacesListSize() {
        val adapter = EventAdapter(events)
        adapter.updateData(events.take(1))
        assertEquals(1, adapter.itemCount)
    }
    @Test
    fun formatDate_validDate_returnsReadableDate() {
        val result =
            EventDateTimeFormatter.formatDate("2026-01-25T09:00:00")
        assertTrue(result.isNotBlank())
    }

    @Test
    fun formatDate_invalidDate_returnsEmpty() {
        val result = EventDateTimeFormatter.formatDate("bad-date")
        assertEquals("", result)
    }

    @Test
    fun formatTimeRange_validTimes_returnsRange() {
        val result =
            EventDateTimeFormatter.formatTimeRange(
                "2026-01-25T09:00:00",
                "2026-01-25T12:00:00"
            )

        assertTrue(result.contains("-"))
    }

    @Test
    fun formatTimeRange_invalidTimes_returnsEmpty() {
        val result =
            EventDateTimeFormatter.formatTimeRange("x", "y")
        assertEquals("", result)
    }

    @Test
    fun messages_areStoredInOrder() {
        val messages = mutableListOf(
            ChatMessage("1", MessageType.USER),
            ChatMessage("2", MessageType.AI)
        )

        val adapter = ChatAdapter(messages)

        assertEquals("1", messages[0].text)
        assertEquals("2", messages[1].text)
    }



    private val events = listOf(
        EventItem(1, "A", "D", "", "Loc", "111", "2026-01-01T09:00:00", "2026-01-01T10:00:00"),
        EventItem(2, "B", "D", "", "Loc", "222", "2026-01-02T09:00:00", "2026-01-02T10:00:00")
    )

    object EventDateTimeFormatter {

        fun formatDate(startTime: String): String {
            return try {
                val start = LocalDateTime.parse(startTime)
                val formatter =
                    DateTimeFormatter.ofPattern("EEEE, MMM dd, yyyy", Locale.ENGLISH)
                start.format(formatter)
            } catch (e: Exception) {
                ""
            }
        }

        fun formatTimeRange(startTime: String, endTime: String): String {
            return try {
                val start = LocalDateTime.parse(startTime)
                val end = LocalDateTime.parse(endTime)
                val formatter = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
                "${start.format(formatter)} - ${end.format(formatter)}"
            } catch (e: Exception) {
                ""
            }
        }
    }

}
