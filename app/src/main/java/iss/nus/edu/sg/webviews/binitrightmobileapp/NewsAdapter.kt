package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.icu.text.SimpleDateFormat
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import iss.nus.edu.sg.webviews.binitrightmobileapp.databinding.ItemNewsCardBinding
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.NewsItem
import java.time.format.DateTimeFormatter
import java.util.Locale

class NewsAdapter(
    private var newsList: List<NewsItem>, // Assuming a simple Data Class for the UI
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

            try {
                val date = inputFormat.parse(news.publishedDate ?: "")
                tvNewsDate.text = date?.let { outputFormat.format(it) } ?: "Recent"
            } catch (e: Exception) {
                android.util.Log.e("NewsAdapter", "Date parsing failed for: ${news.publishedDate}", e)
                tvNewsDate.text = "Recent"
            }

            // Use Glide to load the URL from your SQL into the ImageView
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
        this.newsList = newList
        notifyDataSetChanged()
    }
}