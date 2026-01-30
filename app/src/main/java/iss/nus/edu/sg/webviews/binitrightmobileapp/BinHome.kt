package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import iss.nus.edu.sg.webviews.binitrightmobileapp.databinding.FragmentBinHomeBinding

class BinHomeFragment : Fragment(R.layout.fragment_bin_home) {

    private var _binding: FragmentBinHomeBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentBinHomeBinding.bind(view)

        binding.btnViewNearbyBins.setOnClickListener {
            findNavController().navigate(R.id.action_binHome_to_nearbyBin)
        }

        binding.btnFindRecyclingBins.setOnClickListener {
            findNavController().navigate(R.id.action_binHome_to_findRecyclingBin)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}