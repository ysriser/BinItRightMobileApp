package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.app.Application
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import iss.nus.edu.sg.webviews.binitrightmobileapp.chat.ChatMessage
import iss.nus.edu.sg.webviews.binitrightmobileapp.chat.MessageType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
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

    @Test
    fun onCreateAndBind_setsTextForUserAndAiRows() {
        val messages = mutableListOf(
            ChatMessage("U-msg", MessageType.USER),
            ChatMessage("A-msg", MessageType.AI)
        )
        val adapter = ChatAdapter(messages)
        val parent = FrameLayout(ApplicationProvider.getApplicationContext())

        val userHolder = adapter.onCreateViewHolder(parent, adapter.getItemViewType(0))
        val aiHolder = adapter.onCreateViewHolder(parent, adapter.getItemViewType(1))

        assertTrue(userHolder is ChatAdapter.UserViewHolder)
        assertTrue(aiHolder is ChatAdapter.AiViewHolder)

        adapter.onBindViewHolder(userHolder, 0)
        adapter.onBindViewHolder(aiHolder, 1)

        assertEquals("U-msg", (userHolder as ChatAdapter.UserViewHolder).txt.text.toString())
        assertEquals("A-msg", (aiHolder as ChatAdapter.AiViewHolder).txt.text.toString())
    }

    @Test
    fun addMessage_updatesSizeAndViewType() {
        val adapter = ChatAdapter(mutableListOf(ChatMessage("Hi", MessageType.USER)))

        adapter.addMessage(ChatMessage("Reply", MessageType.AI))

        assertEquals(2, adapter.itemCount)
        assertEquals(2, adapter.getItemViewType(1))
    }
}
