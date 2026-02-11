package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import iss.nus.edu.sg.webviews.binitrightmobileapp.databinding.FragmentScanHomeBinding

class ScanHomeFragment : Fragment() {

    private var _binding: FragmentScanHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScanHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnStartScan.setOnClickListener {
            findNavController().navigate(R.id.action_scanHomeFragment_to_scanItemFragment)
        }

        binding.btnQuestionnaire.setOnClickListener {
            findNavController().navigate(R.id.action_scanHomeFragment_to_questionnaireFragment)
        }

        binding.btnYesIKnow.setOnClickListener {
            findNavController().navigate(R.id.findRecyclingBinFragment)
        }

        binding.btnBackToHome.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
