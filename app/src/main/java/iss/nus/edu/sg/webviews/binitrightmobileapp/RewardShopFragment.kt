package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import iss.nus.edu.sg.webviews.binitrightmobileapp.databinding.FragmentRewardShopBinding
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.Accessory
import iss.nus.edu.sg.webviews.binitrightmobileapp.network.RetrofitClient
import kotlinx.coroutines.launch


class RewardShopFragment : Fragment() {
    private var _binding: FragmentRewardShopBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: RewardShopAdapter
    private var totalPoints: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRewardShopBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // SafeArgs total points passed from Profile
        totalPoints = RewardShopFragmentArgs.fromBundle(requireArguments()).totalPoints
        binding.tvBalance.text = "Your Balance: $totalPoints points"

        binding.rvShop.layoutManager = GridLayoutManager(requireContext(), 2)

        adapter = RewardShopAdapter(
            emptyList(),
            totalPoints,
            onRedeemClick = { item -> redeem(item) },
            onEquipClick = { item -> equip(item) }
        )

        binding.rvShop.adapter = adapter

        loadShopItems()
    }

    private fun loadShopItems() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val res = RetrofitClient.apiService().getRewardShopItems()
                if (res.isSuccessful) {
                    adapter.updateData(res.body() ?: emptyList())
                } else {
                    Toast.makeText(requireContext(), "Failed to load shop", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Connection error", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun redeem(item: Accessory) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val res = RetrofitClient.apiService().redeemRewardShopItem(item.accessoriesId)
                if (res.isSuccessful) {
                    val body = res.body()
                    if (body != null) {
                        totalPoints = body.newTotalPoints
                        binding.tvBalance.text = "Your Balance: $totalPoints points"
                        adapter.updateTotalPoints(totalPoints)
                        Toast.makeText(requireContext(), body.message, Toast.LENGTH_SHORT).show()

                        loadShopItems() // ✅ refresh so item becomes owned and button changes to Equip
                    }
                } else {
                    Toast.makeText(requireContext(), "Redeem failed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Connection error", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun equip(item: Accessory) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val res = RetrofitClient.apiService().equipAccessory(item.accessoriesId)
                if (res.isSuccessful) {
                    Toast.makeText(requireContext(), "Equipped", Toast.LENGTH_SHORT).show()
                    loadShopItems()
                } else {
                    Toast.makeText(requireContext(), "Equip failed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Connection error", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}