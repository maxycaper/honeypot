package com.bar.honeypot.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GalleryItemTest {

    @Test
    fun `test gallery item creation with default values`() {
        val gallery = GalleryItem(name = "Test Gallery")
        
        assertEquals("Test Gallery", gallery.name)
        assertEquals(null, gallery.parentId)
        assertTrue(gallery.children.isEmpty())
        assertTrue(gallery.id.isNotEmpty()) // Should auto-generate ID
    }
    
    @Test
    fun `test gallery item creation with specified values`() {
        val parentId = "parent123"
        val gallery = GalleryItem(
            id = "test123",
            name = "Test Gallery",
            parentId = parentId
        )
        
        assertEquals("test123", gallery.id)
        assertEquals("Test Gallery", gallery.name)
        assertEquals(parentId, gallery.parentId)
        assertTrue(gallery.children.isEmpty())
    }
    
    @Test
    fun `test adding child galleries`() {
        val parentGallery = GalleryItem(id = "parent", name = "Parent Gallery")
        val childGallery = GalleryItem(id = "child", name = "Child Gallery", parentId = "parent")
        
        parentGallery.children.add(childGallery)
        
        assertEquals(1, parentGallery.children.size)
        assertEquals(childGallery, parentGallery.children.first())
    }
    
    @Test
    fun `test gallery equality based on id`() {
        val gallery1 = GalleryItem(id = "same", name = "Gallery 1")
        val gallery2 = GalleryItem(id = "same", name = "Gallery 2")
        val gallery3 = GalleryItem(id = "different", name = "Gallery 1")
        
        // Two galleries with same ID should be considered equal
        assertEquals(gallery1, gallery2)
        
        // Even if names are the same, different IDs mean different galleries
        assertNotEquals(gallery1, gallery3)
    }
}
