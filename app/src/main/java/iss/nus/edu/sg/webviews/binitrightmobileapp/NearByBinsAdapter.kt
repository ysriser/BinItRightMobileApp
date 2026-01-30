package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import iss.nus.edu.sg.webviews.binitrightmobileapp.databinding.ItemRecyclingBinBinding
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.DropOffLocation


class NearByBinsAdapter(
    private val bins: List<DropOffLocation>,
    private val onItemClick: (DropOffLocation) -> Unit
) : RecyclerView.Adapter<NearByBinsAdapter.BinViewHolder>() {

    private var selectedPosition = RecyclerView.NO_POSITION

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BinViewHolder {
        val binding = ItemRecyclingBinBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BinViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BinViewHolder, position: Int) {
        holder.bind(bins[position], position == selectedPosition)
    }

    override fun getItemCount(): Int {
        return bins.size
    }

    inner class BinViewHolder(private val binding: ItemRecyclingBinBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(bin: DropOffLocation, isSelected: Boolean) {

            binding.apply {
                binName.text = bin.name.ifBlank { "Recycling Bin" }
                binAddress.text = " ${bin.address}"
                binPostalCode.text = bin.postalCode
                binDescription.text = bin.description
                binType.text = bin.binType
                //binStatus.text = bin.status
                binDistance.text = " ${"%.1f".format(bin.distanceMeters)} m away"

                // Button visibility logic
                binding.selectButton.visibility = if (isSelected) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

                // Card click selects item
                binding.root.setOnClickListener {
                    val previous = selectedPosition
                    selectedPosition = bindingAdapterPosition

                    if (previous != RecyclerView.NO_POSITION) {
                        notifyItemChanged(previous)
                    }
                    notifyItemChanged(selectedPosition)
                }

                // Button click action
                binding.selectButton.setOnClickListener {
                    onItemClick(bin)
                }
            }
        }
    }
}
