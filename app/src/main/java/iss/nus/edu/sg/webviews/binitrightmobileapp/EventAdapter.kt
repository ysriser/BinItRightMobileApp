package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import iss.nus.edu.sg.webviews.binitrightmobileapp.databinding.ItemEventCardBinding
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.EventItem
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class EventAdapter(
    private var eventList: List<EventItem>
) : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    inner class EventViewHolder(val binding: ItemEventCardBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = ItemEventCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return EventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = eventList[position]
        with(holder.binding) {
            val context = holder.itemView.context
            tvEventTitle.text = event.title
            tvEventDescription.text = event.description
            tvEventLocation.text = event.locationName

            val start = LocalDateTime.parse(event.startTime)
            val end = LocalDateTime.parse(event.endTime)

            val dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMM dd, yyyy", Locale.ENGLISH)
            tvEventDate.text = start.format(dateFormatter)

            val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
            tvEventTime.text = context.getString(
                R.string.event_time_range,
                start.format(timeFormatter),
                end.format(timeFormatter)
            )

            Glide.with(ivEventImage.context)
                .load(event.imageUrl)
                .centerCrop()
                .placeholder(android.R.drawable.progress_horizontal)
                .into(ivEventImage)

            btnGoThere.text = context.getString(R.string.event_go_there_now)
            btnGoThere.setOnClickListener {
                val query = "${event.locationName}, ${event.postalCode}, Singapore"
                val intentUri = "geo:0,0?q=${android.net.Uri.encode(query)}".toUri()
                val mapIntent = Intent(Intent.ACTION_VIEW, intentUri)

                try {
                    holder.itemView.context.startActivity(mapIntent)
                } catch (_: Exception) {
                    val webUri = "https://www.google.com/maps/search/?api=1&query=${android.net.Uri.encode(query)}".toUri()
                    val browserIntent = Intent(Intent.ACTION_VIEW, webUri)
                    holder.itemView.context.startActivity(browserIntent)
                }
            }
        }
    }

    override fun getItemCount() = eventList.size

    fun updateData(newList: List<EventItem>) {
        val oldSize = eventList.size
        eventList = newList
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
