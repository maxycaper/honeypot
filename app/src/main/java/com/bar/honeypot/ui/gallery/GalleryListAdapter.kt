package com.bar.honeypot.ui.gallery

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bar.honeypot.databinding.ItemGalleryBinding

class GalleryListAdapter(
    private val onItemClick: (GalleryItem) -> Unit,
    private val onArrowClick: ((GalleryItem) -> Unit)? = null
) : ListAdapter<GalleryItem, GalleryListAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemGalleryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemGalleryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            // Set click listener for the whole item - captures clicks on any part of the card
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }

            // Set click listener for the arrow icon (this is now redundant with full card clicking)
            binding.arrowIcon.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onArrowClick?.invoke(getItem(position)) ?: onItemClick(getItem(position))
                }
            }
        }

        fun bind(item: GalleryItem) {
            binding.galleryName.text = "${item.name} (${item.itemCount})"
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<GalleryItem>() {
            override fun areItemsTheSame(oldItem: GalleryItem, newItem: GalleryItem): Boolean {
                return oldItem.name == newItem.name
            }

            override fun areContentsTheSame(oldItem: GalleryItem, newItem: GalleryItem): Boolean {
                return oldItem == newItem
            }
        }
    }
}
