package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import iss.nus.edu.sg.webviews.binitrightmobileapp.databinding.ItemRecyclingBinBinding
import iss.nus.edu.sg.webviews.binitrightmobileapp.databinding.ItemRecyclingBinNoButtonBinding
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.DropOffLocation


class NearByBinsAdapter(
    private val bins: List<DropOffLocation>,
    private val onItemClick: (DropOffLocation) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var selectedPosition = RecyclerView.NO_POSITION

    override fun getItemViewType(position: Int): Int {
        return if (position == selectedPosition) VIEW_TYPE_SELECTED else VIEW_TYPE_NORMAL
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_SELECTED) {
            val binding = ItemRecyclingBinBinding.inflate(inflater, parent, false)
            SelectedViewHolder(binding)
        } else {
            val binding = ItemRecyclingBinNoButtonBinding.inflate(inflater, parent, false)
            NormalViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val bin = bins[position]
        when (holder) {
            is SelectedViewHolder -> holder.bind(bin)
            is NormalViewHolder -> holder.bind(bin)
        }
    }

    override fun getItemCount(): Int {
        return bins.size
    }

    private fun selectItem(position: Int) {
        val previous = selectedPosition
        selectedPosition = position
        if (previous != RecyclerView.NO_POSITION) {
            notifyItemChanged(previous)
        }
        notifyItemChanged(selectedPosition)
    }

    inner class SelectedViewHolder(
        private val binding: ItemRecyclingBinBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(bin: DropOffLocation) {

            binding.apply {
                binName.text = bin.name.ifBlank { "Recycling Bin" }
                binAddress.text = " ${bin.address}"
                binPostalCode.text = bin.postalCode
                binDescription.text = bin.description
                binType.text = bin.binType
                //binStatus.text = bin.status
                binDistance.text = " ${"%.1f".format(bin.distanceMeters)} m away"

                // Card click selects item
                binding.root.setOnClickListener {
                    selectItem(bindingAdapterPosition)
                }

                // Button click action
                binding.selectButton.setOnClickListener {
                    onItemClick(bin)
                }
            }
        }
    }

    inner class NormalViewHolder(
        private val binding: ItemRecyclingBinNoButtonBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(bin: DropOffLocation) {
            binding.apply {
                binName.text = bin.name.ifBlank { "Recycling Bin" }
                binAddress.text = " ${bin.address}"
                binPostalCode.text = bin.postalCode
                binDescription.text = bin.description
                binType.text = bin.binType
                //binStatus.text = bin.status
                binDistance.text = " ${"%.1f".format(bin.distanceMeters)} m away"

                // Card click selects item
                binding.root.setOnClickListener {
                    selectItem(bindingAdapterPosition)
                }
            }
        }
    }

    companion object {
        private const val VIEW_TYPE_NORMAL = 0
        private const val VIEW_TYPE_SELECTED = 1
    }
}
