package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.RecycleHistoryModel
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

class RecycleHistoryAdapter : RecyclerView.Adapter<RecycleHistoryAdapter.ViewHolder>() {

    private val items = mutableListOf<RecycleHistoryModel>()

    fun submitList(data: List<RecycleHistoryModel>) {
        val oldSize = items.size
        items.clear()
        items.addAll(data)
        val newSize = items.size

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

    private val singaporeZone: ZoneId = ZoneId.of("Asia/Singapore")
    private val outputFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.ENGLISH)

    fun resolveIcon(categoryName: String): Int {
        val normalized = categoryName.trim().lowercase(Locale.ENGLISH)
        return when {
            normalized.contains("plastic") -> R.drawable.ic_plastic
            normalized.contains("paper") || normalized.contains("cardboard") -> R.drawable.ic_paper
            normalized.contains("e-waste") || normalized.contains("ewaste") || normalized.contains("electronic") -> R.drawable.ic_ewaste
            normalized.contains("lighting") || normalized.contains("lamp") || normalized.contains("bulb") -> R.drawable.ic_lighting
            normalized.contains("other") || normalized.contains("not sure") || normalized.contains("uncertain") || normalized.contains("textile") -> R.drawable.ic_others
            normalized.contains("glass") -> R.drawable.ic_glass
            normalized.contains("metal") -> R.drawable.ic_metal
            else -> R.drawable.ic_recycle
        }
    }

    private fun formatHistoryDate(rawDate: String): String {
        val text = rawDate.trim()
        if (text.isEmpty()) return rawDate

        parseIsoWithZone(text)?.let { return it }
        parseIsoWithoutZone(text)?.let { return it }

        return rawDate
    }

    private fun parseIsoWithZone(text: String): String? {
        return try {
            val instant = if (text.endsWith("Z", ignoreCase = true)) {
                Instant.parse(text)
            } else {
                OffsetDateTime.parse(text).toInstant()
            }
            instant.atZone(singaporeZone).format(outputFormatter)
        } catch (_: DateTimeParseException) {
            null
        }
    }

    private fun parseIsoWithoutZone(text: String): String? {
        val normalized = text.replace(" ", "T")
        return try {
            val utcDateTime = LocalDateTime.parse(normalized)
            utcDateTime
                .atZone(ZoneOffset.UTC)
                .withZoneSameInstant(singaporeZone)
                .format(outputFormatter)
        } catch (_: DateTimeParseException) {
            null
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
        holder.date.text = formatHistoryDate(item.date)
        holder.qty.text = holder.itemView.context.getString(
            R.string.recycle_history_quantity_format,
            item.quantity
        )

        holder.icon.setImageResource(resolveIcon(item.categoryName))
    }

    override fun getItemCount() = items.size
}
