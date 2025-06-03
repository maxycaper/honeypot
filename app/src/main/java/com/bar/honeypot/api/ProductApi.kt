package com.bar.honeypot.api

import android.util.Log
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Data classes for Open Food Facts API response
 */
data class ProductResponse(
    @SerializedName("product") val product: Product? = null,
    @SerializedName("status") val status: Int = 0,
    @SerializedName("status_verbose") val statusVerbose: String? = null
)

data class Product(
    @SerializedName("product_name") val productName: String? = null,
    @SerializedName("product_name_en") val productNameEn: String? = null,
    @SerializedName("generic_name") val genericName: String? = null,
    @SerializedName("generic_name_en") val genericNameEn: String? = null,
    @SerializedName("brands") val brands: String? = null,
    @SerializedName("quantity") val quantity: String? = null,
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("image_front_url") val imageFrontUrl: String? = null,
    @SerializedName("code") val code: String? = null
)

/**
 * Retrofit API interface for Open Food Facts
 */
interface ProductApiService {
    @GET("api/v0/product/{barcode}.json")
    fun getProductInfo(@Path("barcode") barcode: String): Call<ProductResponse>
    
    @GET("api/v0/product/{barcode}.json")
    suspend fun getProductInfoSuspend(@Path("barcode") barcode: String): ProductResponse
}

/**
 * Singleton for accessing the Product API
 */
object ProductApi {
    private const val BASE_URL = "https://world.openfoodfacts.org/"
    private const val TAG = "ProductApi"
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val service: ProductApiService = retrofit.create(ProductApiService::class.java)
} 