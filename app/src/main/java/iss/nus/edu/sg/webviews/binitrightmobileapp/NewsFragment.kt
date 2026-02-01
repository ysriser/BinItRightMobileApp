package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import iss.nus.edu.sg.webviews.binitrightmobileapp.databinding.FragmentNewsBinding

class NewsFragment : Fragment() {

    private var _binding: FragmentNewsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NewsViewModel by viewModels()
    private lateinit var newsAdapter: NewsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        newsAdapter = NewsAdapter { clickedItem ->
            val bundle = Bundle().apply {
                putLong("id", clickedItem.id)
                putString("title", clickedItem.title)
                putString("description", clickedItem.description)
                putString("imageUrl", clickedItem.imageUrl)
                putString("status", clickedItem.status)
            }

            try {
                findNavController().navigate(R.id.newsDetailFragment, bundle)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        binding.rvNews.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = newsAdapter
        }

        viewModel.newsList.observe(viewLifecycleOwner) { list ->
            newsAdapter.submitList(list)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}