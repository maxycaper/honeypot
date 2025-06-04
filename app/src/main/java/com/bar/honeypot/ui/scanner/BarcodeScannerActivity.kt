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

        Log.i(TAG, "Barcode scanner activity started")

        setupBarcodeScanner()

        // Check permissions and start camera
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION
            )
        }

        // Set up the executor for camera operations
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun setupBarcodeScanner() {
        Log.d(TAG, "Setting up barcode scanner with ALL format support")
        
        // Configure scanner to support ALL barcode formats
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
            .enableAllPotentialBarcodes()
            .build()
        
        barcodeScanner = BarcodeScanning.getClient(options)
        Log.d(TAG, "Barcode scanner configured successfully")
    }

    private fun startCamera() {
        Log.d(TAG, "Starting camera")
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }

            // Image analysis for barcode scanning
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetResolution(android.util.Size(1280, 720))
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, BarcodeAnalyzer { barcodes ->
                        processBarcodes(barcodes)
                    })
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
                Log.d(TAG, "Camera started successfully")
            } catch (exc: Exception) {
                Log.e(TAG, "Camera binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }
    
    private fun processBarcodes(barcodes: List<Barcode>) {
        if (barcodes.isNotEmpty()) {
            val barcode = barcodes.first()
            barcode.rawValue?.let { value ->
                val format = getReadableFormat(barcode.format)
                Log.i(TAG, "Barcode detected: '$value' (Format: $format)")
                
                // Process the barcode immediately to avoid multiple processing
                processDetectedBarcode(barcode, value, format)
            }
        }
    }
    
    private fun processDetectedBarcode(barcode: Barcode, value: String, format: String) {
        Log.d(TAG, "Processing barcode: value='$value', format='$format'")
        
        // Check if this is a product barcode that needs API lookup
        if (isProductBarcode(format)) {
            Log.d(TAG, "Product barcode detected - looking up product info")
            lookupProductInfo(value, format) { productName, brand, imageUrl ->
                finishWithBarcodeData(barcode, value, format, productName, brand, imageUrl)
            }
        } else {
            Log.d(TAG, "Non-product barcode detected - returning data immediately")
            finishWithBarcodeData(barcode, value, format, null, null, null)
        }
    }

    private fun isProductBarcode(format: String): Boolean {
        return when (format) {
            "EAN_13", "EAN_8", "UPC_A", "UPC_E" -> true
            "CODE_128" -> true // Many products use CODE_128
            else -> false
        }
    }

    private fun lookupProductInfo(
        barcodeValue: String,
        format: String,
        callback: (String?, String?, String?) -> Unit
    ) {
        // Show loading
        runOnUiThread {
            binding.loadingOverlay.visibility = View.VISIBLE
            binding.loadingText.text = "Looking up product..."
        }
        
        Log.d(TAG, "Starting product lookup for: $barcodeValue")
        
        lifecycleScope.launch {
            try {
                val productInfo = BarcodeInfoService.getProductInfo(barcodeValue)
                
                withContext(Dispatchers.Main) {
                    binding.loadingOverlay.visibility = View.GONE
                }
                
                if (productInfo != null && (productInfo.name.isNotEmpty() || productInfo.brand.isNotEmpty())) {
                    val finalName = when {
                        productInfo.name.isNotEmpty() && productInfo.brand.isNotEmpty() -> 
                            "${productInfo.brand} ${productInfo.name}"
                        productInfo.name.isNotEmpty() -> productInfo.name
                        productInfo.brand.isNotEmpty() -> productInfo.brand
                        else -> null
                    }
                    Log.i(TAG, "Product found: '$finalName'")
                    callback(finalName, productInfo.brand, productInfo.imageUrl)
                } else {
                    Log.d(TAG, "No product info found")
                    callback(null, null, null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Product lookup failed: ${e.message}")
                withContext(Dispatchers.Main) {
                    binding.loadingOverlay.visibility = View.GONE
                }
                callback(null, null, null)
            }
        }
    }

    private fun getReadableFormat(format: Int): String {
        return when (format) {
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
            else -> "UNKNOWN"
        }
    }

    private fun finishWithBarcodeData(
        barcode: Barcode,
        value: String,
        format: String,
        productName: String?,
        brand: String?,
        imageUrl: String?
    ) {
        Log.i(TAG, "Finishing with barcode: '$value' ($format)")
        
        val resultIntent = Intent().apply {
            putExtra(BARCODE_VALUE, value)
            putExtra(BARCODE_FORMAT, format)
            
            // Handle product info
            if (!productName.isNullOrEmpty()) {
                putExtra(BARCODE_PRODUCT_NAME, productName)
                putExtra(BARCODE_TITLE, productName)
            } else if (isProductBarcode(format)) {
                // Default product naming for products without API results
                val defaultName = "$format Product: $value"
                putExtra(BARCODE_PRODUCT_NAME, defaultName)
                putExtra(BARCODE_TITLE, defaultName)
            }
            
            if (!brand.isNullOrEmpty()) {
                putExtra(BARCODE_PRODUCT_BRAND, brand)
            }
            
            if (!imageUrl.isNullOrEmpty()) {
                putExtra(BARCODE_PRODUCT_IMAGE_URL, imageUrl)
            }
            
            // Extract structured data from barcode
            extractStructuredData(barcode, this)
        }
        
        Log.i(TAG, "Barcode processing completed successfully")
        setResult(RESULT_OK, resultIntent)
        finish()
    }
    
    private fun extractStructuredData(barcode: Barcode, intent: Intent) {
        // URL data
        barcode.url?.let { urlInfo ->
            urlInfo.url?.let { intent.putExtra(BARCODE_URL, it) }
            urlInfo.title?.let { intent.putExtra(BARCODE_TITLE, it) }
            if (urlInfo.url != null) {
                intent.putExtra(BARCODE_DESCRIPTION, "Website link")
            }
        }
        
        // Email data
        barcode.email?.let { emailInfo ->
            emailInfo.address?.let { intent.putExtra(BARCODE_EMAIL, it) }
            val emailContent = StringBuilder()
            emailInfo.subject?.let { emailContent.append("Subject: $it\n") }
            emailInfo.body?.let { emailContent.append("Body: $it") }
            if (emailContent.isNotEmpty()) {
                intent.putExtra(BARCODE_DESCRIPTION, emailContent.toString())
            }
        }
        
        // Phone data
        barcode.phone?.let { phoneInfo ->
            phoneInfo.number?.let { intent.putExtra(BARCODE_PHONE, it) }
        }
        
        // SMS data
        barcode.sms?.let { smsInfo ->
            smsInfo.phoneNumber?.let { intent.putExtra(BARCODE_PHONE, it) }
            smsInfo.message?.let { intent.putExtra(BARCODE_SMS, it) }
        }
        
        // WiFi data
        barcode.wifi?.let { wifiInfo ->
            wifiInfo.ssid?.let { intent.putExtra(BARCODE_WIFI_SSID, it) }
            wifiInfo.password?.let { intent.putExtra(BARCODE_WIFI_PASSWORD, it) }
            intent.putExtra(BARCODE_WIFI_TYPE, wifiInfo.encryptionType.toString())
        }
        
        // Geographic coordinates
        barcode.geoPoint?.let { geoInfo ->
            intent.putExtra(BARCODE_GEO_LAT, geoInfo.lat)
            intent.putExtra(BARCODE_GEO_LNG, geoInfo.lng)
        }
        
        // Contact information
        barcode.contactInfo?.let { contactInfo ->
            val name = contactInfo.name?.formattedName ?: "Contact"
            intent.putExtra(BARCODE_TITLE, name)
            
            val contactDetails = StringBuilder()
            contactInfo.emails?.forEach { email ->
                email.address?.let { contactDetails.append("Email: $it\n") }
            }
            contactInfo.phones?.forEach { phone ->
                phone.number?.let { contactDetails.append("Phone: $it\n") }
            }
            contactInfo.addresses?.forEach { address ->
                address.addressLines?.let { lines ->
                    contactDetails.append("Address: ${lines.joinToString(", ")}\n")
                }
            }
            
            if (contactDetails.isNotEmpty()) {
                intent.putExtra(BARCODE_CONTACT_INFO, contactDetails.toString().trim())
            }
        }
        
        // Calendar event
        barcode.calendarEvent?.let { event ->
            event.summary?.let { intent.putExtra(BARCODE_TITLE, it) }
        }
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
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        Log.d(TAG, "Barcode scanner activity destroyed")
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
} 