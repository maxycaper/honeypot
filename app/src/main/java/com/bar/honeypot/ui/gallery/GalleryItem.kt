package com.bar.honeypot.ui.gallery

import java.util.UUID

/**
 * Represents a gallery or sub-gallery. Sub-galleries have a non-null parentId.
 */
data class GalleryItem(
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    var parentId: String? = null, // null for top-level galleries
    val children: MutableList<GalleryItem> = mutableListOf()
)