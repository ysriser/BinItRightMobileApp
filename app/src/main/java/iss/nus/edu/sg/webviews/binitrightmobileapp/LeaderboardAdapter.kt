package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.LeaderboardEntry
import java.util.Locale

class LeaderboardAdapter(private val items: List<LeaderboardEntry>) :
    RecyclerView.Adapter<LeaderboardAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvRank: TextView = view.findViewById(R.id.tv_rank)
        val tvUsername: TextView = view.findViewById(R.id.tv_username)
        val tvQuantity: TextView = view.findViewById(R.id.tv_quantity)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_leaderboard, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvRank.text = holder.itemView.context.getString(
            R.string.leaderboard_rank_number,
            position + 1
        )
        holder.tvUsername.text = item.username
        holder.tvQuantity.text = String.format(Locale.getDefault(), "%d", item.totalQuantity)

        when (position) {
            0 -> holder.tvRank.setTextColor("#FFD700".toColorInt())
            1 -> holder.tvRank.setTextColor("#9E9E9E".toColorInt())
            2 -> holder.tvRank.setTextColor("#CD7F32".toColorInt())
            else -> holder.tvRank.setTextColor("#212121".toColorInt())
        }
    }

    override fun getItemCount() = items.size
}
