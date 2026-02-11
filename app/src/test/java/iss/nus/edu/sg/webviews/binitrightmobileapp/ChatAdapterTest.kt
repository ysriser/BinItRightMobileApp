package iss.nus.edu.sg.webviews.binitrightmobileapp

import iss.nus.edu.sg.webviews.binitrightmobileapp.chat.ChatMessage
import iss.nus.edu.sg.webviews.binitrightmobileapp.chat.MessageType
import org.junit.Assert.assertEquals
import org.junit.Test

class ChatAdapterTest {

    @Test
    fun getItemCount_returnsCorrectSize() {
        val messages = mutableListOf(
            ChatMessage("Hi", MessageType.USER),
            ChatMessage("Hello!", MessageType.AI)
        )

        val adapter = ChatAdapter(messages)

        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun getItemViewType_returnsUserTypeForUserMessage() {
        val messages = mutableListOf(
            ChatMessage("Hi", MessageType.USER)
        )

        val adapter = ChatAdapter(messages)
        val viewType = adapter.getItemViewType(0)

        assertEquals(1, viewType)
    }

    @Test
    fun getItemViewType_returnsAiTypeForAiMessage() {
        val messages = mutableListOf(
            ChatMessage("Hello!", MessageType.AI)
        )

        val adapter = ChatAdapter(messages)
        val viewType = adapter.getItemViewType(0)

        assertEquals(2, viewType)
    }
}