package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
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
        val context = holder.itemView.context

        // 1. Map name to local drawable (e.g., "Blue Cap" -> "blue_cap")
        val drawableName = item.accessories.name.lowercase().replace(" ", "_")
        val resId = context.resources.getIdentifier(drawableName, "drawable", context.packageName)

        if (resId != 0) {
            holder.binding.itemIcon.setImageResource(resId)
        }

        holder.binding.itemName.text = item.accessories.name

        // 2. Selection Styling: Blue border if equipped
        if (item.equipped) {
            holder.binding.accessoryCard.strokeWidth = 6
            holder.binding.accessoryCard.strokeColor = Color.parseColor("#536DFE")
            holder.binding.accessoryCard.setCardBackgroundColor(Color.parseColor("#EEF0FF"))
        } else {
            holder.binding.accessoryCard.strokeWidth = 0
            holder.binding.accessoryCard.setCardBackgroundColor(Color.WHITE)
        }

        // 3. Click listener for the whole card
        holder.binding.root.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<UserAccessory>) {
        this.items = newItems
        notifyDataSetChanged()
    }
}