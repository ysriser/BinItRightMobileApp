//package iss.nus.edu.sg.webviews.binitrightmobileapp
//
//import android.os.Bundle
//import android.view.View
//import androidx.fragment.app.Fragment
//import androidx.navigation.fragment.navArgs
//import androidx.lifecycle.lifecycleScope
//import androidx.navigation.fragment.findNavController
//import com.bumptech.glide.Glide
//import iss.nus.edu.sg.webviews.binitrightmobileapp.databinding.FragmentNewsDetailBinding
//import iss.nus.edu.sg.webviews.binitrightmobileapp.network.RetrofitClient
//import kotlinx.coroutines.launch
//
//class NewsDetailFragment : Fragment(R.layout.fragment_news_detail) {
//
//    private val args: NewsDetailFragmentArgs by navArgs()
//    private var _binding: FragmentNewsDetailBinding? = null
//    private val binding get() = _binding!!
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//        _binding = FragmentNewsDetailBinding.bind(view)
//
//        // Handle the back button click
//        binding.btnBack.setOnClickListener {
//            // This 'pops' the current fragment off the stack and returns to the previous one
//            findNavController().popBackStack()
//        }
//
//        val newsId = args.newsId
//        fetchNewsDetails(newsId)
//    }
//
//    private fun fetchNewsDetails(id: Long) {
//        viewLifecycleOwner.lifecycleScope.launch {
//            try {
//                // You may need to add @GET("api/news/{id}") to your ApiService
//                val response = RetrofitClient.apiService().getNewsById(id)
//                if (response.isSuccessful) {
//                    val news = response.body()
//                    binding.tvDetailTitle.text = news?.name
//                    binding.tvDetailBody.text = news?.description
//
//                    Glide.with(this@NewsDetailFragment)
//                        .load(news?.imageUrl)
//                        .into(binding.ivDetailImage)
//                }
//            } catch (e: Exception) {
//                // Handle error
//            }
//        }
//    }
//}