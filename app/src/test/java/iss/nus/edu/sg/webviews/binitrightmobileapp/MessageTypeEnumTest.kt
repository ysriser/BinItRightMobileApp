package iss.nus.edu.sg.webviews.binitrightmobileapp

import iss.nus.edu.sg.webviews.binitrightmobileapp.chat.MessageType
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageTypeEnumTest {

    @Test
    fun messageType_hasUserAndAi() {
        val values = MessageType.values().toList()

        assertTrue(values.contains(MessageType.USER))
        assertTrue(values.contains(MessageType.AI))
    }
}