package com.bar.honeypot.model

import java.util.Date

data class BarcodeData(
    val value: String,
    val format: String,
    val timestamp: Long = System.currentTimeMillis(),
    val galleryName: String,
    val title: String = "",
    val description: String = "",
    val url: String = "",
    val email: String = "",
    val phone: String = "",
    val smsContent: String = "",
    val wifiSsid: String = "",
    val wifiPassword: String = "",
    val wifiType: String = "",
    val geoLat: Double = 0.0,
    val geoLng: Double = 0.0,
    val productName: String = "",
    val contactInfo: String = ""
) {
    fun getFormattedTimestamp(): String {
        val date = Date(timestamp)
        return date.toString()
    }
    
    fun hasMetadata(): Boolean {
        return title.isNotEmpty() || 
               description.isNotEmpty() || 
               url.isNotEmpty() || 
               email.isNotEmpty() || 
               phone.isNotEmpty() || 
               smsContent.isNotEmpty() || 
               wifiSsid.isNotEmpty() || 
               productName.isNotEmpty() || 
               contactInfo.isNotEmpty() ||
               (geoLat != 0.0 && geoLng != 0.0)
    }
    
    fun getMetadataType(): String {
        return when {
            url.isNotEmpty() -> "URL"
            email.isNotEmpty() -> "Email"
            phone.isNotEmpty() -> "Phone"
            smsContent.isNotEmpty() -> "SMS"
            wifiSsid.isNotEmpty() -> "WiFi"
            geoLat != 0.0 && geoLng != 0.0 -> "Location"
            contactInfo.isNotEmpty() -> "Contact"
            productName.isNotEmpty() -> "Product"
            else -> "Text"
        }
    }
} 