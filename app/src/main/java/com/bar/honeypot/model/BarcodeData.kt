package com.bar.honeypot.model

import java.util.Date

data class BarcodeData(
    val value: String,
    val format: String,
    val timestamp: Long = System.currentTimeMillis(),
    val galleryName: String
) {
    fun getFormattedTimestamp(): String {
        val date = Date(timestamp)
        return date.toString()
    }
} 