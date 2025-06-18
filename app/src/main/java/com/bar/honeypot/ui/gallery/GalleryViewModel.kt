package com.bar.honeypot.ui.gallery

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.bar.honeypot.model.BarcodeData
import com.bar.honeypot.ui.gallery.GalleryItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class GalleryViewModel : ViewModel() {
    
    companion object {
        private const val TAG = "GalleryViewModel"
    }

    private val _barcodes = MutableLiveData<MutableList<BarcodeData>>()
    val barcodes: LiveData<MutableList<BarcodeData>> = _barcodes
    
    private val _currentGalleryName = MutableLiveData<String>()
    val currentGalleryName: LiveData<String> = _currentGalleryName

    // Hierarchical gallery structure (top-level galleries only)
    private val _galleries = MutableLiveData<MutableList<GalleryItem>>(mutableListOf())
    val galleries: LiveData<MutableList<GalleryItem>> = _galleries

    init {
        _barcodes.value = mutableListOf()
        Log.d(TAG, "GalleryViewModel initialized")
    }
    
    fun setGalleryName(name: String) {
        _currentGalleryName.value = name
        Log.d(TAG, "Gallery name set to: '$name'")
    }
    
    /**
     * Adds a barcode to the gallery if it doesn't already exist.
     * @param value The barcode value
     * @param format The barcode format
     * @param metadata Additional barcode metadata
     * @return true if the barcode was added, false if it's a duplicate
     */
    fun addBarcode(
        value: String, 
        format: String,
        title: String = "",
        description: String = "",
        url: String = "",
        email: String = "",
        phone: String = "",
        smsContent: String = "",
        wifiSsid: String = "",
        wifiPassword: String = "",
        wifiType: String = "",
        geoLat: Double = 0.0,
        geoLng: Double = 0.0,
        productName: String = "",
        contactInfo: String = "",
        productImageUrl: String = ""
    ): Boolean {
        val currentList = _barcodes.value ?: mutableListOf()
        
        Log.d(TAG, "Adding barcode: value='$value', format='$format', gallery='${_currentGalleryName.value}'")
        
        // Check if this barcode already exists in the gallery
        if (isDuplicateBarcode(value)) {
            Log.w(TAG, "Duplicate barcode rejected: '$value'")
            return false
        }
        
        val newBarcode = BarcodeData(
            value = value,
            format = format,
            galleryName = _currentGalleryName.value ?: "Unknown Gallery",
            title = title,
            description = description,
            url = url,
            email = email,
            phone = phone,
            smsContent = smsContent,
            wifiSsid = wifiSsid,
            wifiPassword = wifiPassword,
            wifiType = wifiType,
            geoLat = geoLat,
            geoLng = geoLng,
            productName = productName,
            contactInfo = contactInfo,
            productImageUrl = productImageUrl
        )
        
        currentList.add(newBarcode)
        _barcodes.value = currentList
        
        Log.i(TAG, "Barcode added successfully: '$value' ($format) to gallery '${_currentGalleryName.value}'. Total: ${currentList.size}")
        return true
    }
    
    /**
     * Checks if a barcode already exists in the current gallery.
     * @param value The barcode value to check
     * @return true if the barcode already exists, false otherwise
     */
    fun isDuplicateBarcode(value: String): Boolean {
        val currentList = _barcodes.value ?: return false
        val exists = currentList.any { it.value == value }
        if (exists) {
            Log.d(TAG, "Duplicate check: barcode '$value' already exists in gallery")
        }
        return exists
    }
    
    fun deleteBarcode(position: Int) {
        val currentList = _barcodes.value ?: return
        if (position in 0 until currentList.size) {
            val deletedBarcode = currentList[position]
            currentList.removeAt(position)
            _barcodes.value = currentList
            Log.i(TAG, "Barcode deleted: '${deletedBarcode.value}' (${deletedBarcode.format}) from position $position")
        } else {
            Log.w(TAG, "Invalid delete position: $position")
        }
    }
    
    fun saveBarcodes(saveCallback: (String) -> Unit) {
        val barcodesJson = Gson().toJson(_barcodes.value)
        Log.d(TAG, "Saving ${_barcodes.value?.size ?: 0} barcodes to storage")
        saveCallback(barcodesJson)
    }
    
    fun loadBarcodes(galleryName: String, barcodesJson: String?) {
        _currentGalleryName.value = galleryName
        Log.d(TAG, "Loading barcodes for gallery: '$galleryName'")
        
        if (barcodesJson.isNullOrEmpty()) {
            _barcodes.value = mutableListOf()
            Log.d(TAG, "No saved barcodes found for gallery '$galleryName'")
            return
        }
        
        try {
            val type = object : TypeToken<List<BarcodeData>>() {}.type
            val loadedBarcodes: List<BarcodeData> = Gson().fromJson(barcodesJson, type)
            // Filter barcodes for this gallery
            _barcodes.value = loadedBarcodes
                .filter { it.galleryName == galleryName }
                .toMutableList()
            Log.i(TAG, "Loaded ${_barcodes.value?.size ?: 0} barcodes for gallery '$galleryName'")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading barcodes for gallery '$galleryName': ${e.message}", e)
            _barcodes.value = mutableListOf()
        }
    }
    
    /**
     * Updates the title of a barcode at the given position
     * @param position The position of the barcode in the list
     * @param newTitle The new title to set
     * @return true if successful, false otherwise
     */
    fun updateBarcodeTitle(position: Int, newTitle: String): Boolean {
        val currentList = _barcodes.value ?: return false
        
        if (position in 0 until currentList.size) {
            val oldBarcode = currentList[position]
            val updatedBarcode = oldBarcode.copy(title = newTitle)
            currentList[position] = updatedBarcode
            _barcodes.value = currentList
            Log.i(TAG, "Barcode title updated: '${oldBarcode.value}' title changed from '${oldBarcode.title}' to '$newTitle'")
            return true
        }
        
        Log.w(TAG, "Invalid position for title update: $position")
        return false
    }
    
    /**
     * Updates the product image URL of a barcode at the given position
     * @param position The position of the barcode in the list
     * @param productImageUrl The new product image URL to set
     * @return true if successful, false otherwise
     */
    fun updateBarcodeProductImage(position: Int, productImageUrl: String): Boolean {
        val currentList = _barcodes.value ?: return false
        
        if (position in 0 until currentList.size) {
            val oldBarcode = currentList[position]
            val updatedBarcode = oldBarcode.copy(productImageUrl = productImageUrl)
            currentList[position] = updatedBarcode
            _barcodes.value = currentList
            Log.i(TAG, "Barcode product image updated: '${oldBarcode.value}' image set to '$productImageUrl'")
            return true
        }
        
        Log.w(TAG, "Invalid position for product image update: $position")
        return false
    }
    
    /**
     * Removes the product image from a barcode at the given position
     * @param position The position of the barcode in the list
     * @return true if successful, false otherwise
     */
    fun removeBarcodeProductImage(position: Int): Boolean {
        val currentList = _barcodes.value ?: return false
        
        if (position in 0 until currentList.size) {
            val oldBarcode = currentList[position]
            val updatedBarcode = oldBarcode.copy(productImageUrl = "")
            currentList[position] = updatedBarcode
            _barcodes.value = currentList
            Log.i(TAG, "Barcode product image removed: '${oldBarcode.value}'")
            return true
        }
        
        Log.w(TAG, "Invalid position for product image removal: $position")
        return false
    }

    // CRUD for galleries and sub-galleries
    fun addGallery(
        name: String,
        parentId: String? = null,
        prefs: SharedPreferences? = null
    ): GalleryItem {
        val newGallery = GalleryItem(name = name, parentId = parentId)
        if (parentId == null) {
            // Top-level gallery
            _galleries.value?.add(newGallery)
        } else {
            // Sub-gallery: find parent and add as child
            val parent = findGalleryById(parentId)
            parent?.children?.add(newGallery)
        }
        _galleries.value = _galleries.value // Trigger observers
        prefs?.let { saveGalleries(it) }
        return newGallery
    }

    fun removeGallery(id: String, prefs: SharedPreferences? = null) {
        // Remove from top-level or from parent's children
        _galleries.value?.removeAll { it.id == id }
        _galleries.value?.forEach { parent ->
            parent.children.removeAll { it.id == id }
        }
        _galleries.value = _galleries.value
        prefs?.let { saveGalleries(it) }
    }

    fun renameGallery(id: String, newName: String, prefs: SharedPreferences? = null) {
        findGalleryById(id)?.name = newName
        _galleries.value = _galleries.value
        prefs?.let { saveGalleries(it) }
    }

    fun findGalleryById(id: String): GalleryItem? {
        _galleries.value?.forEach { gallery ->
            if (gallery.id == id) return gallery
            gallery.children.forEach { child ->
                if (child.id == id) return child
            }
        }
        return null
    }

    fun saveGalleries(prefs: SharedPreferences) {
        val editor = prefs.edit()
        val galleriesJson = Gson().toJson(_galleries.value)
        editor.putString("hierarchical_galleries", galleriesJson)
        editor.apply()
    }

    fun loadGalleries(prefs: SharedPreferences) {
        val galleriesJson = prefs.getString("hierarchical_galleries", null)
        if (!galleriesJson.isNullOrEmpty()) {
            try {
                val type = object : TypeToken<MutableList<GalleryItem>>() {}.type
                val loaded: MutableList<GalleryItem> = Gson().fromJson(galleriesJson, type)
                _galleries.value = loaded
            } catch (e: Exception) {
                _galleries.value = mutableListOf()
            }
        } else {
            _galleries.value = mutableListOf()
        }
    }
}
