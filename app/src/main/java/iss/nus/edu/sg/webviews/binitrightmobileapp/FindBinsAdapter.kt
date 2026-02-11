package iss.nus.edu.sg.todo.samplebin

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import iss.nus.edu.sg.webviews.binitrightmobileapp.R
import iss.nus.edu.sg.webviews.binitrightmobileapp.databinding.ItemFindRecyclingBinBinding
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.DropOffLocation

class FindBinsAdapter(
    private val bins: List<DropOffLocation>
) : RecyclerView.Adapter<FindBinsAdapter.ViewHolder>() {

    fun formatDistance(distanceMeters: Double, context: android.content.Context): String {
        return context.getString(R.string.find_bins_distance_km, distanceMeters / 1000.0)
    }

    fun mapBinType(type: String): String {
        return when (type.uppercase()) {
            "BLUEBIN" -> "General"
            "EWASTE" -> "E-Waste"
            "LIGHTING", "LAMP" -> "Lighting"
            else -> type
        }
    }

    fun buildNavigationUri(lat: Double, lng: Double) = "google.navigation:q=$lat,$lng".toUri()

    fun notifyForDataChange(oldSize: Int, newSize: Int) {
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

    inner class ViewHolder(private val binding: ItemFindRecyclingBinBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(bin: DropOffLocation) {
            binding.txtName.text = bin.name
            binding.txtAddress.text = bin.address
            binding.txtDistance.text = formatDistance(bin.distanceMeters, binding.root.context)
            binding.txtHours.text = binding.root.context.getString(R.string.find_bins_open_24_7)
            binding.txtType.text = mapBinType(bin.binType)

            binding.btnDirections.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, buildNavigationUri(bin.latitude, bin.longitude)).apply {
                    setPackage("com.google.android.apps.maps")
                }
                it.context.startActivity(intent)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFindRecyclingBinBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount() = bins.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(bins[position])
    }
}
