package iss.nus.edu.sg.webviews.binitrightmobileapp

import iss.nus.edu.sg.webviews.binitrightmobileapp.chat.ChatMessage
import iss.nus.edu.sg.webviews.binitrightmobileapp.chat.MessageType
import org.junit.Assert.assertEquals
import org.junit.Test

class ChatMessageModelTest {

    @Test
    fun chatMessage_holdsTextAndType() {
        val msg = ChatMessage("Hello", MessageType.USER)

        assertEquals("Hello", msg.text)
        assertEquals(MessageType.USER, msg.type)
    }
}