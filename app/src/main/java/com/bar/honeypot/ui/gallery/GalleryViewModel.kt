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
    
    fun addBarcode(value: String, format: String) {
        val currentList = _barcodes.value ?: mutableListOf()
        val newBarcode = BarcodeData(
            value = value,
            format = format,
            galleryName = _currentGalleryName.value ?: "Unknown Gallery"
        )
        currentList.add(newBarcode)
        _barcodes.value = currentList
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
}