package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.R
import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import iss.nus.edu.sg.webviews.binitrightmobileapp.databinding.ItemEventCardBinding
import iss.nus.edu.sg.webviews.binitrightmobileapp.databinding.ItemNewsCardBinding
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
            tvEventTitle.text = event.title
            tvEventDescription.text = event.description
            tvEventLocation.text = event.locationName

            // Parse the ISO strings
            val start = LocalDateTime.parse(event.startTime)
            val end = LocalDateTime.parse(event.endTime)

            // Format for Date: Saturday, Jan 25, 2026
            val dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMM dd, yyyy", Locale.ENGLISH)
            tvEventDate.text = start.format(dateFormatter)

            // Format for Time Range: 9:00 AM - 12:00 PM
            val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
            tvEventTime.text = "${start.format(timeFormatter)} - ${end.format(timeFormatter)}"

            Glide.with(ivEventImage.context)
                .load(event.imageUrl)
                .centerCrop()
                .placeholder(android.R.drawable.progress_horizontal)
                .into(ivEventImage)

            // Trigger Google Maps Intent
            btnGoThere.text = "Go there now"
            btnGoThere.setOnClickListener {
                val query = "${event.locationName}, ${event.postalCode}, Singapore"
                // The "geo:0,0?q=" URI is a standard Android convention for location searches
                val intentUri = Uri.parse("geo:0,0?q=${Uri.encode(query)}")

                // Create the Intent without specifying a package
                val mapIntent = Intent(Intent.ACTION_VIEW, intentUri)

                try {
                    holder.itemView.context.startActivity(mapIntent)
                } catch (e: Exception) {
                    // Fallback: If no map apps are installed, open in the browser
                    val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(query)}")
                    val browserIntent = Intent(Intent.ACTION_VIEW, webUri)
                    holder.itemView.context.startActivity(browserIntent)
                }
            }
        }
    }

    override fun getItemCount() = eventList.size

    fun updateData(newList: List<EventItem>) {
        this.eventList = newList
        notifyDataSetChanged()
    }
}