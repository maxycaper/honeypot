package com.bar.honeypot.ui.gallery

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.bar.honeypot.model.BarcodeData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class GalleryViewModel : ViewModel() {

    private val _barcodes = MutableLiveData<MutableList<BarcodeData>>()
    val barcodes: LiveData<MutableList<BarcodeData>> = _barcodes
    
    private val _currentGalleryName = MutableLiveData<String>()
    val currentGalleryName: LiveData<String> = _currentGalleryName
    
    init {
        _barcodes.value = mutableListOf()
    }
    
    fun setGalleryName(name: String) {
        _currentGalleryName.value = name
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
        
        // Check if this barcode already exists in the gallery
        if (isDuplicateBarcode(value)) {
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
        return true
    }
    
    /**
     * Checks if a barcode already exists in the current gallery.
     * @param value The barcode value to check
     * @return true if the barcode already exists, false otherwise
     */
    fun isDuplicateBarcode(value: String): Boolean {
        val currentList = _barcodes.value ?: return false
        return currentList.any { it.value == value }
    }
    
    fun deleteBarcode(position: Int) {
        val currentList = _barcodes.value ?: return
        if (position in 0 until currentList.size) {
            currentList.removeAt(position)
            _barcodes.value = currentList
        }
    }
    
    fun saveBarcodes(saveCallback: (String) -> Unit) {
        val barcodesJson = Gson().toJson(_barcodes.value)
        saveCallback(barcodesJson)
    }
    
    fun loadBarcodes(galleryName: String, barcodesJson: String?) {
        _currentGalleryName.value = galleryName
        if (barcodesJson.isNullOrEmpty()) {
            _barcodes.value = mutableListOf()
            return
        }
        
        try {
            val type = object : TypeToken<List<BarcodeData>>() {}.type
            val loadedBarcodes: List<BarcodeData> = Gson().fromJson(barcodesJson, type)
            // Filter barcodes for this gallery
            _barcodes.value = loadedBarcodes
                .filter { it.galleryName == galleryName }
                .toMutableList()
        } catch (e: Exception) {
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
            val updatedBarcode = currentList[position].copy(title = newTitle)
            currentList[position] = updatedBarcode
            _barcodes.value = currentList
            return true
        }
        
        return false
    }
}