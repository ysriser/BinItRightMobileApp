package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import iss.nus.edu.sg.webviews.binitrightmobileapp.databinding.ItemAchievementBinding
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.Achievement

class AchievementAdapter(private val onItemClick: (Achievement) -> Unit) :
    ListAdapter<Achievement, AchievementAdapter.AchievementViewHolder>(AchievementDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AchievementViewHolder {
        val binding =
            ItemAchievementBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AchievementViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AchievementViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)

        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    class AchievementViewHolder(private val binding: ItemAchievementBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Achievement) {
            binding.tvName.text = item.name
            binding.tvDescription.text = item.description

            binding.ivBadge.load(item.badgeIconUrl) {
                crossfade(true)
                placeholder(android.R.drawable.ic_menu_help)
                error(android.R.drawable.ic_lock_idle_lock)
            }

            if (item.isUnlocked) {
                // UNLOCKED STATE
                binding.root.alpha = 1.0f
                binding.tvTapToView.isVisible = true
                binding.ivBadge.colorFilter = null

                val bg = GradientDrawable()
                bg.shape = GradientDrawable.OVAL
                bg.setColor(Color.parseColor("#00C853"))
                binding.ivStatusIcon.background = bg
                binding.ivStatusIcon.setImageResource(android.R.drawable.btn_star_big_on)
                binding.ivStatusIcon.setColorFilter(Color.WHITE)

            } else {
                // LOCKED STATE
                binding.root.alpha = 0.6f
                binding.tvTapToView.isVisible = false

                val matrix = ColorMatrix()
                matrix.setSaturation(0f)
                binding.ivBadge.colorFilter = ColorMatrixColorFilter(matrix)

                val bg = GradientDrawable()
                bg.shape = GradientDrawable.OVAL
                bg.setColor(Color.parseColor("#CFD8DC"))
                binding.ivStatusIcon.background = bg
                binding.ivStatusIcon.setImageResource(android.R.drawable.ic_lock_lock)
                binding.ivStatusIcon.setColorFilter(Color.WHITE)
            }
        }
    }

    class AchievementDiffCallback : DiffUtil.ItemCallback<Achievement>() {
        override fun areItemsTheSame(oldItem: Achievement, newItem: Achievement) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Achievement, newItem: Achievement) =
            oldItem == newItem
    }
}