package iss.nus.edu.sg.todo.samplebin

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import iss.nus.edu.sg.webviews.binitrightmobileapp.databinding.ItemFindRecyclingBinBinding
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.DropOffLocation

class FindBinsAdapter(
    private val bins: List<DropOffLocation>
) : RecyclerView.Adapter<FindBinsAdapter.ViewHolder>() {

    fun formatDistance(distanceMeters: Double): String {
        return "%.1f km away".format(distanceMeters / 1000)
    }

    fun mapBinType(type: String): String {
        return when (type.uppercase()) {
            "BLUEBIN" -> "General"
            "EWASTE" -> "E-Waste"
            "LIGHTING", "LAMP" -> "Lighting"
            else -> type
        }
    }

    fun buildNavigationUri(lat: Double, lng: Double): Uri {
        return Uri.parse("google.navigation:q=$lat,$lng")
    }

    inner class ViewHolder(private val binding: ItemFindRecyclingBinBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(bin: DropOffLocation) {
            binding.txtName.text = bin.name
            binding.txtAddress.text = bin.address
            binding.txtDistance.text = formatDistance(bin.distanceMeters)
            binding.txtHours.text = "Open 24/7"
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
