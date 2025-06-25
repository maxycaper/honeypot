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

            // FALLBACK: Generate a default product name based on the barcode format
            // This ensures we always return a product name even if the API fails
            val defaultName = generateDefaultProductName(code)
            Log.d(TAG, "Using fallback product name: '$defaultName'")

            return@withContext ProductInfo(
                code = code,
                name = defaultName,
                brand = "",
                imageUrl = ""
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error looking up product: ${e.message}", e)
            null
        }
    }

    /**
     * Generate a default product name when the API lookup fails
     */
    private fun generateDefaultProductName(code: String): String {
        // Determine what type of product this might be based on the barcode prefix
        return when {
            // UPC-A (12 digits) or EAN-13 (13 digits)
            code.length == 12 || code.length == 13 -> {
                when {
                    // Food products often start with 0
                    code.startsWith("0") -> "Food Product: $code"
                    // Books often have EAN-13 starting with 978
                    code.startsWith("978") -> "Book: $code"
                    // Periodicals often have EAN-13 starting with 977
                    code.startsWith("977") -> "Magazine: $code"
                    // Drugs/pharmaceuticals often start with 3
                    code.startsWith("3") -> "Pharmacy Item: $code"
                    // Default product
                    else -> "Product: $code"
                }
            }
            // EAN-8 (8 digits)
            code.length == 8 -> "Small Product: $code"
            // Default
            else -> "Product: $code"
        }
    }
}
