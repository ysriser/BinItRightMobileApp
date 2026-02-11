package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import iss.nus.edu.sg.webviews.binitrightmobileapp.databinding.ItemNewsCardBinding
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.NewsItem
import java.text.SimpleDateFormat
import java.util.Locale

class NewsAdapter(
    private var newsList: List<NewsItem>,
    private val onItemClick: (NewsItem) -> Unit
) : RecyclerView.Adapter<NewsAdapter.NewsViewHolder>() {

    inner class NewsViewHolder(val binding: ItemNewsCardBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val binding = ItemNewsCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return NewsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        val news = newsList[position]
        with(holder.binding) {
            tvNewsTitle.text = news.name
            tvNewsDescription.text = news.description

            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

            tvNewsDate.text = runCatching {
                val date = inputFormat.parse(news.publishedDate.orEmpty())
                date?.let { outputFormat.format(it) }
            }.getOrNull() ?: root.context.getString(R.string.news_recent)

            Glide.with(ivNewsImage.context)
                .load(news.imageUrl)
                .centerCrop()
                .placeholder(android.R.drawable.progress_horizontal)
                .into(ivNewsImage)

            root.setOnClickListener { onItemClick(news) }
        }
    }

    override fun getItemCount() = newsList.size

    fun updateData(newList: List<NewsItem>) {
        val oldSize = newsList.size
        newsList = newList
        val newSize = newList.size

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
