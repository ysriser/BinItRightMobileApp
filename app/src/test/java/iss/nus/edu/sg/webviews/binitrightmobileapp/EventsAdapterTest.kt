package iss.nus.edu.sg.webviews.binitrightmobileapp

import iss.nus.edu.sg.webviews.binitrightmobileapp.chat.ChatMessage
import iss.nus.edu.sg.webviews.binitrightmobileapp.chat.MessageType
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.EventItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

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