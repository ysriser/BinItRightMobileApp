package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import iss.nus.edu.sg.webviews.binitrightmobileapp.databinding.ItemNewsBinding
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.NewsItem

class NewsAdapter(private val onItemClick: (NewsItem) -> Unit) :
    ListAdapter<NewsItem, NewsAdapter.NewsViewHolder>(NewsDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val binding = ItemNewsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NewsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NewsViewHolder(private val binding: ItemNewsBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: NewsItem) {
            binding.tvTitle.text = item.title
            binding.tvDescription.text = item.description
            binding.tvStatusBadge.text = item.status

            binding.ivNewsImage.load(item.imageUrl) {
                crossfade(true)
                placeholder(android.R.drawable.ic_menu_gallery)
                error(android.R.drawable.ic_dialog_alert)
            }

            if (item.status.equals("Upcoming", ignoreCase = true)) {
                binding.tvStatusBadge.setBackgroundColor(Color.parseColor("#00C853"))
            } else {
                binding.tvStatusBadge.setBackgroundColor(Color.parseColor("#757575"))
            }

            binding.root.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    class NewsDiffCallback : DiffUtil.ItemCallback<NewsItem>() {
        override fun areItemsTheSame(oldItem: NewsItem, newItem: NewsItem) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: NewsItem, newItem: NewsItem) = oldItem == newItem
    }
}