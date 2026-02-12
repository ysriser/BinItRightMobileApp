package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import iss.nus.edu.sg.webviews.binitrightmobileapp.databinding.FragmentEventsBinding
import iss.nus.edu.sg.webviews.binitrightmobileapp.network.RetrofitClient
import kotlinx.coroutines.launch

class EventsFragment : Fragment() {

    private var _binding: FragmentEventsBinding? = null
    private val binding get() = _binding!!

    private lateinit var eventAdapter: EventAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEventsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        fetchUpcomingEvents() // Initial load when fragment opens
    }

    private fun setupRecyclerView() {
        eventAdapter = EventAdapter(mutableListOf())
        binding.recyclerViewEvent.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = eventAdapter
        }
    }

    private fun setupListeners() {
        // Check In Button
        binding.btnCheckIn.setOnClickListener {
            Toast.makeText(context, "Check-in scanner coming soon!", Toast.LENGTH_SHORT).show()
        }

        // Simple Refresh Button
        binding.btnRefresh.setOnClickListener {
            fetchUpcomingEvents()
        }
    }

    private fun fetchUpcomingEvents() {
        // lifecycleScope ensures the call is cancelled if the fragment is closed
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // RetrofitClient already has the AuthInterceptor attached
                val response = RetrofitClient.apiService().getUpcomingEvents()

                if (response.isSuccessful) {
                    response.body()?.let { list ->
                        eventAdapter.updateData(list)
                    }
                } else {
                    Toast.makeText(context, "Server error: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                // Connection errors (e.g., backend off or IP 10.0.2.2 unreachable)
                Toast.makeText(context, "Network error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}