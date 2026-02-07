package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import iss.nus.edu.sg.webviews.binitrightmobileapp.chat.ChatMessage
import iss.nus.edu.sg.webviews.binitrightmobileapp.chat.MessageType
import iss.nus.edu.sg.webviews.binitrightmobileapp.databinding.FragmentChatBinding
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.ChatRequest
import iss.nus.edu.sg.webviews.binitrightmobileapp.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatFragment : Fragment(R.layout.fragment_chat) {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private lateinit var chatAdapter: ChatAdapter
    private val messages = mutableListOf<ChatMessage>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentChatBinding.bind(view)

        chatAdapter = ChatAdapter(messages)
        binding.chatRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.chatRecyclerView.adapter = chatAdapter

        binding.sendBtn.setOnClickListener {
            sendMessage(binding.input.text.toString())
        }
    }

    private fun sendMessage(text: String) {
        val msg = text.trim()
        if (msg.isEmpty()) return

        addMessage(msg, MessageType.USER)
        binding.input.text.clear()

        getAIResponseFromBackend(msg)
    }

    private fun addMessage(text: String, type: MessageType) {
        messages.add(ChatMessage(text, type))
        chatAdapter.notifyItemInserted(messages.size - 1)
        binding.chatRecyclerView.scrollToPosition(messages.size - 1)
    }

    private fun getAIResponseFromBackend(userText: String) {
        binding.sendBtn.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val resp = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService().chat(ChatRequest(userText))
                }
                addMessage(resp.reply.trim(), MessageType.AI)

            } catch (e: Exception) {
                addMessage("Server error: ${e.message}", MessageType.AI)
            } finally {
                binding.sendBtn.isEnabled = true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
