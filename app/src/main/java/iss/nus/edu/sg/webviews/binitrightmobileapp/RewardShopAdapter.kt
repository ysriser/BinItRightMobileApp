package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.Accessory


class RewardShopAdapter(
    private var items: List<Accessory>,
    private var totalPoints: Int,
    private val onRedeemClick: (Accessory) -> Unit
) : RecyclerView.Adapter<RewardShopAdapter.VH>() {

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val iv: ImageView = itemView.findViewById(R.id.ivAccessory)
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val tvPoints: TextView = itemView.findViewById(R.id.tvPoints)
        val btn: Button = itemView.findViewById(R.id.btnRedeem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reward_shop, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]

        // Text
        holder.tvName.text = item.name
        holder.tvPoints.text = "${item.requiredPoints} pts"

        // Image from drawable name based on item.name
        val drawableName = item.name
            .trim()
            .lowercase()
            .replace(Regex("\\s+"), "_")

        val resId = holder.itemView.context.resources.getIdentifier(
            drawableName, "drawable", holder.itemView.context.packageName
        )

        holder.iv.setImageResource(
            if (resId != 0) resId else R.drawable.ic_launcher_foreground
        )

        // Redeem logic
        val enough = totalPoints >= item.requiredPoints
        holder.btn.text = if (enough) "Redeem" else "Not enough"
        holder.btn.isEnabled = enough
        holder.btn.alpha = if (enough) 1f else 0.5f

        holder.btn.setOnClickListener {
            if (enough) onRedeemClick(item)
        }
    }

    fun updateData(newItems: List<Accessory>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun updateTotalPoints(newPoints: Int) {
        totalPoints = newPoints
        notifyDataSetChanged()
    }
}