package com.bar.honeypot.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.awaitResponse

/**
 * Data class for product info results
 */
data class ProductInfo(
    val code: String,
    val name: String,
    val brand: String,
    val imageUrl: String
)

/**
 * Service for retrieving barcode product information
 */
object BarcodeInfoService {
    private const val TAG = "BarcodeInfoService"
    
    /**
     * Get product information for a barcode
     * @param code The barcode to look up
     * @return ProductInfo if found, null otherwise
     */
    suspend fun getProductInfo(code: String): ProductInfo? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Looking up product info for barcode: $code")
            
            try {
                val response = ProductApi.service.getProductInfoSuspend(code)
                val product = response.product
                
                if (product != null) {
                    // Extract product info
                    val rawName = product.productName 
                        ?: product.productNameEn
                        ?: product.genericName
                        ?: product.genericNameEn
                        ?: ""
                        
                    val brand = product.brands ?: ""
                    val imageUrl = product.imageUrl ?: product.imageFrontUrl ?: ""
                    
                    Log.d(TAG, "Found product - name: '$rawName', brand: '$brand'")
                    
                    return@withContext ProductInfo(
                        code = code,
                        name = rawName,
                        brand = brand,
                        imageUrl = imageUrl
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "API request failed: ${e.message}")
            }
            
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error looking up product: ${e.message}", e)
            null
        }
    }
} 