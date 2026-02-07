package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import iss.nus.edu.sg.webviews.binitrightmobileapp.databinding.FragmentAvatarCustomizationBinding
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.UserAccessory
import kotlinx.coroutines.launch
import iss.nus.edu.sg.webviews.binitrightmobileapp.network.RetrofitClient


class AvatarCustomizationFragment : Fragment() {

    private var _binding: FragmentAvatarCustomizationBinding? = null
    private val binding get() = _binding!!
    private lateinit var avatarAdapter: AvatarAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAvatarCustomizationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Close FAB navigation
        binding.btnClose.setOnClickListener {
            findNavController().navigateUp()
        }

        // Setup 3-column Grid
        binding.accessoriesGrid.layoutManager = GridLayoutManager(requireContext(), 2)

        // Initialize adapter with empty list
        avatarAdapter = AvatarAdapter(emptyList()) { selectedItem ->
            handleEquip(selectedItem)
        }
        binding.accessoriesGrid.adapter = avatarAdapter

        loadAccessories()
    }

    private fun loadAccessories() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService().getMyAccessories()
                if (response.isSuccessful) {
                    response.body()?.let { items ->
                        avatarAdapter.updateData(items)
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load data", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun handleEquip(item: UserAccessory) {
        if (item.equipped) return

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val response =
                        RetrofitClient.apiService().equipAccessory(item.accessories.accessoriesId)

                    if (response.isSuccessful) {
                        loadAccessories() // refresh to show new equipped highlight
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Connection error", Toast.LENGTH_SHORT).show()
                }
            }
        }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}