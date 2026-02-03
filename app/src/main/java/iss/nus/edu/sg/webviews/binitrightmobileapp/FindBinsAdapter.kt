package iss.nus.edu.sg.todo.samplebin

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import iss.nus.edu.sg.webviews.binitrightmobileapp.databinding.ItemFindRecyclingBinBinding
import iss.nus.edu.sg.webviews.binitrightmobileapp.Model.DropOffLocation

class FindBinsAdapter(
    private val bins: List<DropOffLocation>
) : RecyclerView.Adapter<FindBinsAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemFindRecyclingBinBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(bin: DropOffLocation) {
            binding.txtName.text = bin.name
            binding.txtAddress.text = bin.address
            binding.txtDistance.text =
                "%.1f km away".format(bin.distanceMeters / 1000)

            binding.txtHours.text = "Open 24/7"

            binding.txtType.text = when (bin.binType) {
                "BLUEBIN" -> "General"
                "EWASTE" -> "E-Waste"
                "LAMP" -> "Lighting"
                else -> bin.binType
            }

            binding.btnDirections.setOnClickListener {
                val uri =
                    Uri.parse("google.navigation:q=${bin.latitude},${bin.longitude}")
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                    setPackage("com.google.android.apps.maps")
                }
                it.context.startActivity(intent)
            }
        }}

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
