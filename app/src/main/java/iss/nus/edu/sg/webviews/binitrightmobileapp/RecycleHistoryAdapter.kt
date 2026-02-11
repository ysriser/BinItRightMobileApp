package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.RecycleHistoryModel

class RecycleHistoryAdapter : RecyclerView.Adapter<RecycleHistoryAdapter.ViewHolder>() {

    private val items = mutableListOf<RecycleHistoryModel>()

    fun submitList(data: List<RecycleHistoryModel>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    fun resolveIcon(categoryName: String): Int {
        return when (categoryName) {
            "Plastic" -> R.drawable.ic_plastic
            "E-Waste" -> R.drawable.ic_ewaste
            "Glass" -> R.drawable.ic_glass
            else -> R.drawable.ic_recycle
        }
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.imgCategory)
        val category: TextView = view.findViewById(R.id.txtCategory)
        val date: TextView = view.findViewById(R.id.txtDate)
        val qty: TextView = view.findViewById(R.id.txtQuantity)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recycle_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.category.text = item.categoryName
        holder.date.text = item.date
        holder.qty.text = "x${item.quantity}"

        holder.icon.setImageResource(resolveIcon(item.categoryName))
    }

    override fun getItemCount() = items.size
}