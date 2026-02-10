package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.LeaderboardEntry

class LeaderboardAdapter(private val items: List<LeaderboardEntry>) :
    RecyclerView.Adapter<LeaderboardAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvRank: TextView = view.findViewById(R.id.tv_rank)
        val tvUsername: TextView = view.findViewById(R.id.tv_username)
        val tvQuantity: TextView = view.findViewById(R.id.tv_quantity)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_leaderboard, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvRank.text = (position + 1).toString()
        holder.tvUsername.text = item.username
        holder.tvQuantity.text = item.totalQuantity.toString()

        when (position) {
            0 -> holder.tvRank.setTextColor(Color.parseColor("#FFD700")) // 金
            1 -> holder.tvRank.setTextColor(Color.parseColor("#9E9E9E")) // 银
            2 -> holder.tvRank.setTextColor(Color.parseColor("#CD7F32")) // 铜
            else -> holder.tvRank.setTextColor(Color.parseColor("#212121"))
        }
    }

    override fun getItemCount() = items.size
}