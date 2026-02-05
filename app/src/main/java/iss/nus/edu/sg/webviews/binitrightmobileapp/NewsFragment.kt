package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import iss.nus.edu.sg.webviews.binitrightmobileapp.databinding.FragmentNewsBinding
import kotlinx.coroutines.launch
import iss.nus.edu.sg.webviews.binitrightmobileapp.network.RetrofitClient


class NewsFragment : Fragment(R.layout.fragment_news) {

    private var _binding: FragmentNewsBinding? = null
    private val binding get() = _binding!!
    private lateinit var newsAdapter: NewsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentNewsBinding.bind(view)

        setupRecyclerView()
        fetchNews()
    }

    private fun setupRecyclerView() {
        newsAdapter = NewsAdapter(emptyList()) { selectedNews ->
            // Create the action with the specific ID from the clicked item
            val action = NewsFragmentDirections.actionNavNewsToNewsDetailFragment(selectedNews.newsId)
            // Navigate
            findNavController().navigate(action)        }

        binding.recyclerViewNews.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = newsAdapter
        }
    }

    private fun fetchNews() {
        // lifecycleScope ensures the call is cancelled if the fragment is closed
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // RetrofitClient already has the AuthInterceptor attached
                val response = RetrofitClient.instance.getAllNews()

                if (response.isSuccessful) {
                    response.body()?.let { list ->
                        newsAdapter.updateData(list)
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