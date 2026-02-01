package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import coil.load
import iss.nus.edu.sg.webviews.binitrightmobileapp.databinding.FragmentNewsDetailBinding

class NewsDetailFragment : Fragment() {

    private var _binding: FragmentNewsDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewsDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val title = arguments?.getString("title")
        val description = arguments?.getString("description")
        val imageUrl = arguments?.getString("imageUrl")
        val status = arguments?.getString("status")

        binding.tvDetailTitle.text = title
        binding.tvDetailDescription.text = description
        binding.tvDetailStatus.text = status

        if (status.equals("Upcoming", ignoreCase = true)) {
            binding.tvDetailStatus.setBackgroundColor(Color.parseColor("#00C853"))
        } else {
            binding.tvDetailStatus.setBackgroundColor(Color.parseColor("#757575"))
        }

        if (imageUrl != null) {
            binding.ivDetailImage.load(imageUrl) {
                placeholder(android.R.drawable.ic_menu_gallery)
                crossfade(true)
            }
        }

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}