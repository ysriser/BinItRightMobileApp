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
        val context = holder.itemView.context

        // Text
        holder.tvName.text = item.name
        holder.tvPoints.text = context.getString(R.string.reward_points_short, item.requiredPoints)
        holder.iv.setImageResource(AvatarAssetResolver.drawableForName(item.name))

        // Redeem logic
        // --- STATE LOGIC: equipped / owned / redeemable ---
        val isOwned = item.owned

        // important: clear old click due to RecyclerView reuse
        holder.btn.setOnClickListener(null)
        holder.itemView.alpha = 1f
        holder.itemView.isClickable = true

        when {
            isOwned -> {
                holder.btn.text = context.getString(R.string.reward_owned)
                holder.btn.isEnabled = false
                holder.btn.alpha = 0.7f
                holder.btn.setOnClickListener(null)
            }

            totalPoints >= item.requiredPoints -> {
                holder.btn.text = context.getString(R.string.reward_redeem)
                holder.btn.isEnabled = true
                holder.btn.alpha = 1f
                holder.btn.setOnClickListener { onRedeemClick(item) }
            }

            else -> {
                holder.btn.text = context.getString(R.string.reward_not_enough)
                holder.btn.isEnabled = false
                holder.btn.alpha = 0.5f
                holder.itemView.alpha = 0.4f
                holder.itemView.isClickable = false
            }
        }

    }

    fun updateData(newItems: List<Accessory>) {
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

    fun updateTotalPoints(newPoints: Int) {
        totalPoints = newPoints
        if (itemCount > 0) {
            notifyItemRangeChanged(0, itemCount)
        }
    }
}
