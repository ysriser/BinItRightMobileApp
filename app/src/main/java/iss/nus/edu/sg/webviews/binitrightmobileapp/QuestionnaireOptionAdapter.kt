package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import iss.nus.edu.sg.webviews.binitrightmobileapp.databinding.RowOptionBinding

class QuestionnaireOptionAdapter(private val onOptionClick: (OptionNode) -> Unit) :
    ListAdapter<OptionNode, QuestionnaireOptionAdapter.OptionViewHolder>(OptionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OptionViewHolder {
        val binding = RowOptionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OptionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OptionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class OptionViewHolder(private val binding: RowOptionBinding) :
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(option: OptionNode) {
            binding.tvOptionText.text = option.text
            
            if (option.id == "BACK_ACTION") {
                binding.tvOptionText.setTextColor(Color.parseColor("#546E7A")) // Darker Blue-Grey for contrast
                binding.root.background.setTint(Color.parseColor("#F1F8FB")) // Match Question Card
                // Could also change icon if we had reference to ImageView
            } else {
                binding.tvOptionText.setTextColor(Color.parseColor("#212121"))
                binding.root.background.setTintList(null) // Reset tint
            }

            binding.root.setOnClickListener {
                onOptionClick(option)
            }
        }
    }

    class OptionDiffCallback : DiffUtil.ItemCallback<OptionNode>() {
        override fun areItemsTheSame(oldItem: OptionNode, newItem: OptionNode): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: OptionNode, newItem: OptionNode): Boolean {
            return oldItem == newItem
        }
    }
}
