package com.bar.honeypot.ui.gallery

import android.graphics.Color
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bar.honeypot.R
import com.bar.honeypot.ui.gallery.GalleryItem

class GalleryDrawerAdapter(
    private var galleries: List<GalleryItem>,
    private val listener: GalleryDrawerListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface GalleryDrawerListener {
        fun onGalleryClicked(gallery: GalleryItem)
        fun onCreateSubGallery(parent: GalleryItem)
        fun onRenameGallery(gallery: GalleryItem)
        fun onExportGallery(gallery: GalleryItem)
        fun onDeleteGallery(gallery: GalleryItem)
    }

    // Track expanded state for each parent gallery
    private val expandedParents = mutableSetOf<String>()

    // Keep track of currently selected gallery
    private var currentGalleryId: String? = null

    // Flattened list for display (parents and visible children)
    private fun getDisplayList(): List<Pair<GalleryItem, Int>> {
        val result = mutableListOf<Pair<GalleryItem, Int>>()
        for (parent in galleries) {
            result.add(parent to 0)
            if (expandedParents.contains(parent.id)) {
                for (child in parent.children) {
                    result.add(child to 1)
                }
            }
        }
        return result
    }

    override fun getItemCount(): Int = getDisplayList().size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_gallery_drawer, parent, false)
        return GalleryViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val (item, indentLevel) = getDisplayList()[position]
        (holder as GalleryViewHolder).bind(item, indentLevel)
    }

    fun updateGalleries(newGalleries: List<GalleryItem>) {
        this.galleries = newGalleries
        notifyDataSetChanged()
    }

    fun setCurrentGallery(galleryId: String) {
        if (currentGalleryId != galleryId) {
            currentGalleryId = galleryId
            notifyDataSetChanged()
        }
    }

    inner class GalleryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameText: TextView = itemView.findViewById(R.id.text_gallery_name)
        private val expandIcon: ImageView = itemView.findViewById(R.id.icon_expand)
        private val menuButton: ImageButton = itemView.findViewById(R.id.btn_gallery_settings)
        private val container: View = itemView.findViewById(R.id.container_gallery_item)

        fun bind(item: GalleryItem, indentLevel: Int) {
            nameText.text = item.name
            // Indent sub-galleries
            val params = container.layoutParams as ViewGroup.MarginLayoutParams
            params.leftMargin = if (indentLevel == 1) 48 else 0
            container.layoutParams = params

            // Expand/collapse icon for parents
            if (item.parentId == null && item.children.isNotEmpty()) {
                expandIcon.visibility = View.VISIBLE
                expandIcon.setImageResource(
                    if (expandedParents.contains(item.id)) R.drawable.ic_expand_less else R.drawable.ic_expand_more
                )
                expandIcon.setOnClickListener {
                    if (expandedParents.contains(item.id)) expandedParents.remove(item.id)
                    else expandedParents.add(item.id)
                    notifyDataSetChanged()
                }
            } else {
                expandIcon.visibility = View.INVISIBLE
            }

            // 3-dots menu
            menuButton.setOnClickListener { v ->
                val popup = PopupMenu(v.context, v)
                popup.menuInflater.inflate(R.menu.menu_gallery_drawer_item, popup.menu)
                // Hide sub-gallery creation for sub-galleries
                if (item.parentId != null) {
                    popup.menu.findItem(R.id.action_create_subgallery).isVisible = false
                }
                popup.setOnMenuItemClickListener { menuItem: MenuItem ->
                    when (menuItem.itemId) {
                        R.id.action_create_subgallery -> listener.onCreateSubGallery(item)
                        R.id.action_rename_gallery -> listener.onRenameGallery(item)
                        R.id.action_export_gallery -> listener.onExportGallery(item)
                        R.id.action_delete_gallery -> listener.onDeleteGallery(item)
                    }
                    true
                }
                popup.show()
            }

            // Click to navigate
            container.setOnClickListener {
                listener.onGalleryClicked(item)
            }

            // Highlight current gallery
            if (currentGalleryId == item.id) {
                container.setBackgroundResource(R.drawable.selected_gallery_background)
                nameText.setTextColor(Color.WHITE)
            } else {
                container.background = null
                nameText.setTextColor(Color.LTGRAY)
            }
        }
    }
}
