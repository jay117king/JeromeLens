package com.jeromelens.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jeromelens.app.data.ClipEntity
import com.jeromelens.app.databinding.ItemClipBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ClipAdapter(
    private val onCopy: (String) -> Unit,
    private val onFavorite: (ClipEntity) -> Unit,
    private val onDelete: (ClipEntity) -> Unit
) : ListAdapter<ClipEntity, ClipAdapter.ClipViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClipViewHolder {
        val binding = ItemClipBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ClipViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ClipViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ClipViewHolder(private val binding: ItemClipBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(clip: ClipEntity) {
            binding.clipText.text = clip.text
            val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
            binding.clipTimestamp.text = sdf.format(Date(clip.timestamp))

            val category = clip.category?.takeIf { it.isNotBlank() }
            if (category != null) {
                binding.clipCategory?.visibility = View.VISIBLE
                binding.clipCategory?.text = category
            } else {
                binding.clipCategory?.visibility = View.GONE
            }

            binding.btnFavorite.setImageResource(
                if (clip.isFavorite) android.R.drawable.btn_star_big_on
                else android.R.drawable.btn_star_big_off
            )

            binding.root.setOnClickListener { onCopy(clip.text) }
            binding.btnFavorite.setOnClickListener { onFavorite(clip) }
            binding.btnDelete.setOnClickListener { onDelete(clip) }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<ClipEntity>() {
        override fun areItemsTheSame(old: ClipEntity, new: ClipEntity) = old.id == new.id
        override fun areContentsTheSame(old: ClipEntity, new: ClipEntity) = old == new
    }
}
