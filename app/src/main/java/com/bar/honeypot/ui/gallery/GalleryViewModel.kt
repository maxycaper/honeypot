package com.bar.honeypot.ui.gallery

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.bar.honeypot.model.BarcodeData
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
        Log.d(TAG, "Product info: title='$title', productName='$productName'")
        Log.e(
            "PRODUCT_DEBUG",
            "VIEWMODEL ADD: title='$title', productName='$productName', value='$value'"
        )
        
        // Check if this barcode already exists in the gallery
        if (isDuplicateBarcode(value)) {
            Log.w(TAG, "Duplicate barcode rejected: '$value'")
            return false
        }

        // If title is empty but product name exists, use product name as title
        val finalTitle = if (title.isEmpty() && productName.isNotEmpty()) {
            Log.d(TAG, "Using product name as title: '$productName'")
            productName
        } else {
            title
        }

        // Ensure product name is preserved even if title is also set
        val finalProductName = if (productName.isNotEmpty()) {
            productName
        } else if (finalTitle.isNotEmpty() && finalTitle.startsWith("Product:")) {
            // If title is a product title but productName is empty, use title as productName too
            finalTitle
        } else {
            ""
        }

        Log.e(
            "PRODUCT_DEBUG",
            "VIEWMODEL FINAL: title='$finalTitle', productName='$finalProductName'"
        )

        val newBarcode = BarcodeData(
            value = value,
            format = format,
            galleryName = _currentGalleryName.value ?: "Unknown Gallery",
            title = finalTitle,
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
            productName = finalProductName,
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

        // Log all barcode product names to debug persistence issues
        _barcodes.value?.forEachIndexed { index, barcode ->
            Log.e(
                "PREFS_DEBUG",
                "SAVING[$index]: value='${barcode.value}', title='${barcode.title}', productName='${barcode.productName}'"
            )

            // Add with HONEYPOT_DEBUG tag
            Log.e(
                "HONEYPOT_DEBUG",
                "PREFS_BARCODE[$index]: value='${barcode.value}', title='${barcode.title}', productName='${barcode.productName}'"
            )
        }

        // Log a sample of the JSON to verify serialization
        val jsonSample =
            if (barcodesJson.length > 100) barcodesJson.substring(0, 100) + "..." else barcodesJson
        Log.e("PREFS_DEBUG", "JSON_SAVE: $jsonSample")
        Log.e("HONEYPOT_DEBUG", "PREFS_JSON: $jsonSample")

        saveCallback(barcodesJson)
    }
    
    fun loadBarcodes(galleryName: String, barcodesJson: String?) {
        _currentGalleryName.value = galleryName
        Log.d(TAG, "Loading barcodes for gallery: '$galleryName'")
        
        if (barcodesJson.isNullOrEmpty()) {
            _barcodes.value = mutableListOf()
            Log.d(TAG, "No saved barcodes found for gallery '$galleryName'")
            Log.e("PREFS_DEBUG", "LOAD: No saved barcodes found for gallery '$galleryName'")
            Log.e(
                "HONEYPOT_DEBUG",
                "PREFS_EMPTY: No saved barcodes found for gallery '$galleryName'"
            )
            return
        }

        // Log a sample of the loaded JSON
        val jsonSample =
            if (barcodesJson.length > 100) barcodesJson.substring(0, 100) + "..." else barcodesJson
        Log.e("PREFS_DEBUG", "JSON_LOAD: $jsonSample")
        Log.e("HONEYPOT_DEBUG", "PREFS_LOADING: $jsonSample")

        try {
            val type = object : TypeToken<List<BarcodeData>>() {}.type
            val loadedBarcodes: List<BarcodeData> = Gson().fromJson(barcodesJson, type)
            // Filter barcodes for this gallery
            _barcodes.value = loadedBarcodes
                .filter { it.galleryName == galleryName }
                .toMutableList()

            // Log all loaded barcode product names
            _barcodes.value?.forEachIndexed { index, barcode ->
                Log.e(
                    "PREFS_DEBUG",
                    "LOADED[$index]: value='${barcode.value}', title='${barcode.title}', productName='${barcode.productName}'"
                )

                // Add with HONEYPOT_DEBUG tag
                Log.e(
                    "HONEYPOT_DEBUG",
                    "PREFS_LOADED[$index]: value='${barcode.value}', title='${barcode.title}', productName='${barcode.productName}'"
                )
            }

            Log.i(TAG, "Loaded ${_barcodes.value?.size ?: 0} barcodes for gallery '$galleryName'")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading barcodes for gallery '$galleryName': ${e.message}", e)
            Log.e("PREFS_DEBUG", "LOAD_ERROR: ${e.message}", e)
            Log.e("HONEYPOT_DEBUG", "PREFS_LOAD_ERROR: ${e.message}", e)
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

    /**
     * Updates an existing barcode with product information
     * @param barcodeValue The barcode value to match
     * @param productName The product name to set
     * @param productBrand The product brand to set
     * @param productImageUrl The product image URL to set
     * @return true if the barcode was found and updated, false otherwise
     */
    fun updateBarcodeProductInfo(
        barcodeValue: String,
        productName: String,
        productBrand: String = "",
        productImageUrl: String = ""
    ): Boolean {
        try {
            Log.e(
                "HONEYPOT_DEBUG",
                "PREFS_UPDATE_START: Attempting to update barcode '$barcodeValue' with product='$productName'"
            )

            // Get current list safely
            val currentList = _barcodes.value ?: mutableListOf()

            // Find the barcode by value
            val position = currentList.indexOfFirst { it.value == barcodeValue }
            if (position < 0) {
                Log.e(
                    "HONEYPOT_DEBUG",
                    "PREFS_UPDATE_FAILED: Barcode '$barcodeValue' not found in gallery"
                )
                return false
            }

            Log.e("HONEYPOT_DEBUG", "PREFS_UPDATING: Barcode '$barcodeValue' at position $position")

            val oldBarcode = currentList[position]
            Log.e(
                "HONEYPOT_DEBUG",
                "PREFS_BEFORE: title='${oldBarcode.title}', productName='${oldBarcode.productName}'"
            )

            // Create updated barcode with copy
            val updatedBarcode = oldBarcode.copy(
                title = if (oldBarcode.title.isNullOrEmpty()) productName else oldBarcode.title,
                description = if (productBrand.isNotEmpty()) "Brand: $productBrand" else oldBarcode.description,
                productImageUrl = if (productImageUrl.isNotEmpty()) productImageUrl else oldBarcode.productImageUrl,
                productName = productName
            )

            currentList[position] = updatedBarcode
            _barcodes.value = currentList

            Log.i(
                TAG,
                "Updated product info for barcode: '${oldBarcode.value}' - name: '$productName'"
            )
            Log.e("HONEYPOT_DEBUG", "PREFS_UPDATE_SUCCESS: Barcode updated successfully")
            return true
        } catch (e: Exception) {
            Log.e(
                "HONEYPOT_DEBUG",
                "PREFS_UPDATE_CRITICAL: Unexpected exception in update process: ${e.message}",
                e
            )
            return false
        }
    }
}
