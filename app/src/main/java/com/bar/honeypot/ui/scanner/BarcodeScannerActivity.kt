package com.bar.honeypot.ui.scanner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bar.honeypot.R
import com.bar.honeypot.api.BarcodeInfoService
import com.bar.honeypot.api.ProductApi
import com.bar.honeypot.api.ProductResponse
import com.bar.honeypot.databinding.ActivityBarcodeScannerBinding
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BarcodeScannerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBarcodeScannerBinding
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var barcodeScanner: BarcodeScanner

    companion object {
        private const val TAG = "BarcodeScanner"
        private const val REQUEST_CAMERA_PERMISSION = 10
        const val BARCODE_VALUE = "barcode_value"
        const val BARCODE_FORMAT = "barcode_format"
        const val BARCODE_TITLE = "barcode_title"
        const val BARCODE_DESCRIPTION = "barcode_description"
        const val BARCODE_URL = "barcode_url"
        const val BARCODE_EMAIL = "barcode_email"
        const val BARCODE_PHONE = "barcode_phone"
        const val BARCODE_SMS = "barcode_sms"
        const val BARCODE_WIFI_SSID = "barcode_wifi_ssid"
        const val BARCODE_WIFI_PASSWORD = "barcode_wifi_password"
        const val BARCODE_WIFI_TYPE = "barcode_wifi_type"
        const val BARCODE_GEO_LAT = "barcode_geo_lat"
        const val BARCODE_GEO_LNG = "barcode_geo_lng"
        const val BARCODE_PRODUCT_NAME = "barcode_product_name"
        const val BARCODE_CONTACT_INFO = "barcode_contact_info"
        const val BARCODE_PRODUCT_BRAND = "barcode_product_brand"
        const val BARCODE_PRODUCT_IMAGE_URL = "barcode_product_image_url"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBarcodeScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Request camera permissions if not already granted
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION
            )
        }

        // Set up barcode scanner with enhanced options for large barcodes
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_ALL_FORMATS
            )
            .enableAllPotentialBarcodes() // Enable detection of all potential barcodes for better large barcode scanning
            .build()
        barcodeScanner = BarcodeScanning.getClient(options)

        // Set up the executor for camera operations
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            // Image analysis for barcode scanning with enhanced resolution
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetResolution(android.util.Size(1280, 720)) // Higher resolution for better large barcode detection
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, BarcodeAnalyzer { barcodes ->
                        if (barcodes.isNotEmpty()) {
                            for (barcode in barcodes) {
                                barcode.rawValue?.let { value ->
                                    val format = getReadableFormat(barcode.format)
                                    Log.d(TAG, "Barcode found: $value, format: $format")
                                    extractAndReturnBarcodeData(barcode, value, format)
                                    return@BarcodeAnalyzer
                                }
                            }
                        }
                    })
                }

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer
                )

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun getReadableFormat(format: Int): String {
        return when (format) {
            Barcode.FORMAT_UNKNOWN -> "Unknown"
            Barcode.FORMAT_CODE_128 -> "CODE_128"
            Barcode.FORMAT_CODE_39 -> "CODE_39"
            Barcode.FORMAT_CODE_93 -> "CODE_93"
            Barcode.FORMAT_CODABAR -> "CODABAR"
            Barcode.FORMAT_DATA_MATRIX -> "DATA_MATRIX"
            Barcode.FORMAT_EAN_13 -> "EAN_13"
            Barcode.FORMAT_EAN_8 -> "EAN_8"
            Barcode.FORMAT_ITF -> "ITF"
            Barcode.FORMAT_QR_CODE -> "QR_CODE"
            Barcode.FORMAT_UPC_A -> "UPC_A"
            Barcode.FORMAT_UPC_E -> "UPC_E"
            Barcode.FORMAT_PDF417 -> "PDF417"
            Barcode.FORMAT_AZTEC -> "AZTEC"
            else -> "Unknown format: $format"
        }
    }

    private fun extractAndReturnBarcodeData(barcode: Barcode, value: String, format: String) {
        // Check if this might be a product barcode (EAN, UPC)
        if (isProductBarcode(format)) {
            // Use the coroutine-based approach similar to the example code
            lookupProductInfoCoroutine(value, format) { productName, brand, imageUrl ->
                finishWithBarcodeData(barcode, value, format, productName, brand, imageUrl)
            }
        } else {
            // Not a product barcode, return data immediately
            finishWithBarcodeData(barcode, value, format, null, null, null)
        }
    }

    private fun isProductBarcode(format: String): Boolean {
        return format.contains("EAN") || 
               format.contains("UPC") || 
               format == "UPC_A" || 
               format == "UPC_E" || 
               format.contains("CODE_128")
    }

    private fun lookupProductInfo(
        barcode: String, 
        format: String,
        callback: (String?, String?, String?) -> Unit
    ) {
        // Show loading indicator
        runOnUiThread {
            binding.loadingOverlay.visibility = View.VISIBLE
            binding.loadingText.text = "Looking up product..."
        }

        Log.d(TAG, "Looking up product info for barcode: $barcode")

        // Make API call to get product info
        ProductApi.service.getProductInfo(barcode).enqueue(object : Callback<ProductResponse> {
            override fun onResponse(call: Call<ProductResponse>, response: Response<ProductResponse>) {
                Log.d(TAG, "API Response received: ${response.isSuccessful}, code: ${response.code()}")
                
                // Log the raw response body if available
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    Log.d(TAG, "Raw response: $responseBody")
                    
                    val product = responseBody?.product
                    Log.d(TAG, "Product object: $product")
                    
                    // Get product info - try multiple fields for name and image
                    val rawProductName = product?.productName 
                        ?: product?.productNameEn
                        ?: product?.genericName
                        ?: product?.genericNameEn
                        
                    val brand = product?.brands
                    
                    // Format the product name with brand if available (similar to the working example)
                    val productName = if (!brand.isNullOrEmpty() && !rawProductName.isNullOrEmpty()) {
                        "$brand $rawProductName"
                    } else {
                        rawProductName
                    }
                    
                    val imageUrl = product?.imageUrl 
                        ?: product?.imageFrontUrl
                    
                    Log.d(TAG, "Extracted data - raw product name: '$rawProductName', brand: '$brand', formatted name: '$productName', imageUrl: '$imageUrl'")
                    
                    // Hide loading indicator
                    runOnUiThread {
                        binding.loadingOverlay.visibility = View.GONE
                    }
                    
                    if (productName != null && productName.isNotEmpty()) {
                        Log.d(TAG, "Product found: $productName, brand: $brand, imageUrl: $imageUrl")
                    } else {
                        Log.d(TAG, "No product name found for barcode: $barcode")
                        
                        // Attempt fallback to any available product info
                        val fallbackName = if (brand != null && brand.isNotEmpty()) {
                            Log.d(TAG, "Using brand as fallback product name: $brand")
                            brand
                        } else {
                            Log.d(TAG, "No fallback product info available, using default")
                            "Product: $barcode"
                        }
                        
                        // If no product name was found but we have a valid response, check the response status
                        responseBody?.let {
                            Log.d(TAG, "API response status: ${it.status}, message: ${it.statusVerbose}")
                        }
                        
                        // Return with fallback name
                        callback(fallbackName, brand, imageUrl)
                        return
                    }
                    
                    // Return the result
                    callback(productName, brand, imageUrl)
                } else {
                    Log.e(TAG, "API request failed: ${response.code()} - ${response.message()}")
                    
                    // Hide loading indicator
                    runOnUiThread {
                        binding.loadingOverlay.visibility = View.GONE
                    }
                    
                    // Return null results
                    callback(null, null, null)
                }
            }

            override fun onFailure(call: Call<ProductResponse>, t: Throwable) {
                // Hide loading indicator
                runOnUiThread {
                    binding.loadingOverlay.visibility = View.GONE
                }
                
                Log.e(TAG, "Error looking up product: ${t.message}", t)
                // Return null results
                callback(null, null, null)
            }
        })
    }
    
    private fun finishWithBarcodeData(
        barcode: Barcode, 
        value: String, 
        format: String,
        productName: String?,
        brand: String?,
        imageUrl: String?
    ) {
        Log.d(TAG, "Finishing with barcode data: value=$value, format=$format, productName=$productName, brand=$brand")
        
        val resultIntent = Intent().apply {
            putExtra(BARCODE_VALUE, value)
            putExtra(BARCODE_FORMAT, format)
            
            // Add product info if available
            if (!productName.isNullOrEmpty()) {
                putExtra(BARCODE_PRODUCT_NAME, productName)
                // Use product name as title by default
                putExtra(BARCODE_TITLE, productName)
                Log.d(TAG, "Set product name: $productName")
            } else if (format.contains("EAN") || format.contains("UPC") || format.contains("CODE_128")) {
                // For product barcodes with no API result
                val defaultName = "Product: $value"
                putExtra(BARCODE_PRODUCT_NAME, defaultName)
                Log.d(TAG, "Set default product name: $defaultName")
            }
            
            if (!brand.isNullOrEmpty()) {
                putExtra(BARCODE_PRODUCT_BRAND, brand)
                // Only override title with brand if specifically desired
                // putExtra(BARCODE_TITLE, brand)
                Log.d(TAG, "Set product brand: $brand")
            }
            
            if (!imageUrl.isNullOrEmpty()) {
                putExtra(BARCODE_PRODUCT_IMAGE_URL, imageUrl)
                Log.d(TAG, "Set product image URL: $imageUrl")
            }
            
            // Extract URL data
            barcode.url?.let { urlInfo ->
                urlInfo.url?.let { putExtra(BARCODE_URL, it) }
                urlInfo.title?.let { putExtra(BARCODE_TITLE, it) }
                if (urlInfo.url != null) {
                    putExtra(BARCODE_DESCRIPTION, "Website link")
                }
            }
            
            // Extract email data
            barcode.email?.let { emailInfo ->
                emailInfo.address?.let { putExtra(BARCODE_EMAIL, it) }
                if (emailInfo.subject?.isNotEmpty() == true || emailInfo.body?.isNotEmpty() == true) {
                    val emailContent = StringBuilder()
                    emailInfo.subject?.let { emailContent.append("Subject: $it\n") }
                    emailInfo.body?.let { emailContent.append("Body: $it") }
                    putExtra(BARCODE_DESCRIPTION, emailContent.toString())
                }
            }
            
            // Extract phone data
            barcode.phone?.let { phoneInfo ->
                phoneInfo.number?.let { putExtra(BARCODE_PHONE, it) }
            }
            
            // Extract SMS data
            barcode.sms?.let { smsInfo ->
                smsInfo.phoneNumber?.let { putExtra(BARCODE_PHONE, it) }
                smsInfo.message?.let { putExtra(BARCODE_SMS, it) }
            }
            
            // Extract WiFi data
            barcode.wifi?.let { wifiInfo ->
                wifiInfo.ssid?.let { putExtra(BARCODE_WIFI_SSID, it) }
                wifiInfo.password?.let { putExtra(BARCODE_WIFI_PASSWORD, it) }
                putExtra(BARCODE_WIFI_TYPE, wifiInfo.encryptionType.toString())
            }
            
            // Extract geographic coordinates
            barcode.geoPoint?.let { geoInfo ->
                putExtra(BARCODE_GEO_LAT, geoInfo.lat)
                putExtra(BARCODE_GEO_LNG, geoInfo.lng)
            }
            
            // Extract product information
            barcode.calendarEvent?.let {
                val eventInfo = StringBuilder()
                it.summary?.let { summary -> eventInfo.append(summary) }
                putExtra(BARCODE_TITLE, eventInfo.toString())
            }
            
            // Extract contact information
            barcode.contactInfo?.let { contactInfo ->
                val name = contactInfo.name?.formattedName ?: "Contact"
                putExtra(BARCODE_TITLE, name)
                
                val contactDetails = StringBuilder()
                contactInfo.emails?.forEach { email ->
                    email.address?.let { addr -> 
                        contactDetails.append("Email: $addr\n") 
                    }
                }
                contactInfo.phones?.forEach { phone ->
                    phone.number?.let { num -> 
                        contactDetails.append("Phone: $num\n") 
                    }
                }
                contactInfo.addresses?.forEach { address ->
                    address.addressLines?.let { lines -> 
                        contactDetails.append("Address: ${lines.joinToString(", ")}\n") 
                    }
                }
                
                if (contactDetails.isNotEmpty()) {
                    putExtra(BARCODE_CONTACT_INFO, contactDetails.toString().trim())
                }
            }
        }
        
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        baseContext, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    "Camera permissions are required to use the scanner",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private inner class BarcodeAnalyzer(private val onBarcodesDetected: (List<Barcode>) -> Unit) :
        ImageAnalysis.Analyzer {

        @androidx.camera.core.ExperimentalGetImage
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(
                    mediaImage,
                    imageProxy.imageInfo.rotationDegrees
                )
                
                barcodeScanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        if (barcodes.isNotEmpty()) {
                            onBarcodesDetected(barcodes)
                        }
                    }
                    .addOnFailureListener {
                        Log.e(TAG, "Barcode scanning failed", it)
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            } else {
                imageProxy.close()
            }
        }
    }

    private fun lookupProductInfoCoroutine(
        barcode: String, 
        format: String,
        callback: (String?, String?, String?) -> Unit
    ) {
        // Show loading indicator
        runOnUiThread {
            binding.loadingOverlay.visibility = View.VISIBLE
            binding.loadingText.text = "Looking up product..."
        }

        Log.d(TAG, "Looking up product info using coroutines for barcode: $barcode")
        
        lifecycleScope.launch {
            try {
                val productInfo = BarcodeInfoService.getProductInfo(barcode)
                
                // Hide loading indicator
                withContext(Dispatchers.Main) {
                    binding.loadingOverlay.visibility = View.GONE
                }
                
                if (productInfo != null) {
                    Log.d(TAG, "Product found: name=${productInfo.name}, brand=${productInfo.brand}, imageUrl=${productInfo.imageUrl}")
                    
                    // Format the product name with brand if both are available
                    val formattedName = if (productInfo.brand.isNotEmpty() && productInfo.name.isNotEmpty()) {
                        "${productInfo.brand} ${productInfo.name}"
                    } else if (productInfo.name.isNotEmpty()) {
                        productInfo.name
                    } else if (productInfo.brand.isNotEmpty()) {
                        productInfo.brand
                    } else {
                        null
                    }
                    
                    callback(formattedName, productInfo.brand, productInfo.imageUrl)
                } else {
                    Log.d(TAG, "No product found for barcode: $barcode")
                    callback(null, null, null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error looking up product: ${e.message}", e)
                
                // Hide loading indicator
                withContext(Dispatchers.Main) {
                    binding.loadingOverlay.visibility = View.GONE
                }
                
                callback(null, null, null)
            }
        }
    }
} 