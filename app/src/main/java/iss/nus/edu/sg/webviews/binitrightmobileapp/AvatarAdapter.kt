package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView
import iss.nus.edu.sg.webviews.binitrightmobileapp.databinding.ItemAccessoryBinding
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.UserAccessory

class AvatarAdapter(
    private var items: List<UserAccessory>,
    private val onItemClick: (UserAccessory) -> Unit
) : RecyclerView.Adapter<AvatarAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemAccessoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAccessoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.binding.itemIcon.setImageResource(
            AvatarAssetResolver.drawableForName(item.accessories.name)
        )
        holder.binding.itemName.text = item.accessories.name

        if (item.equipped) {
            holder.binding.accessoryCard.strokeWidth = 6
            holder.binding.accessoryCard.strokeColor = "#536DFE".toColorInt()
            holder.binding.accessoryCard.setCardBackgroundColor("#EEF0FF".toColorInt())

            holder.binding.root.alpha = 0.7f
            holder.binding.root.setOnClickListener(null)
            holder.binding.root.isClickable = false
        } else {
            holder.binding.accessoryCard.strokeWidth = 0
            holder.binding.accessoryCard.setCardBackgroundColor(Color.WHITE)

            holder.binding.root.alpha = 1f
            holder.binding.root.isClickable = true
            holder.binding.root.setOnClickListener { onItemClick(item) }
        }
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<UserAccessory>) {
        val oldSize = items.size
        items = newItems
        val newSize = newItems.size

        when {
            oldSize == 0 && newSize > 0 -> notifyItemRangeInserted(0, newSize)
            newSize == 0 && oldSize > 0 -> notifyItemRangeRemoved(0, oldSize)
            else -> {
                val common = minOf(oldSize, newSize)
                if (common > 0) {
                    notifyItemRangeChanged(0, common)
                }
                if (newSize > oldSize) {
                    notifyItemRangeInserted(oldSize, newSize - oldSize)
                } else if (oldSize > newSize) {
                    notifyItemRangeRemoved(newSize, oldSize - newSize)
                }
            }
        }
    }
}
