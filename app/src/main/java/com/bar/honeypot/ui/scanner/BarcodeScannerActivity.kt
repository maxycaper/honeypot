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
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import com.bar.honeypot.R
import com.bar.honeypot.api.BarcodeInfoService
import com.bar.honeypot.databinding.ActivityBarcodeScannerBinding
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.LuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.Result
import com.google.zxing.NotFoundException
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.camera.core.FocusMeteringAction
import com.google.mlkit.vision.barcode.common.Barcode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import androidx.appcompat.app.AlertDialog
import com.google.mlkit.vision.common.InputImage

@OptIn(ExperimentalCamera2Interop::class)
class BarcodeScannerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBarcodeScannerBinding
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var barcodeScanner: BarcodeScanner
    private var isProcessingBarcode = false
    private var camera: Camera? = null
    private var isFocusing = false
    private var focusAchieved = false

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

            // Preview with Camera2Interop for continuous autofocus
            val previewBuilder = Preview.Builder()

            val camera2PreviewExtender = Camera2Interop.Extender(previewBuilder)
            camera2PreviewExtender.setCaptureRequestOption(
                android.hardware.camera2.CaptureRequest.CONTROL_AF_MODE,
                android.hardware.camera2.CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
            )
            val preview = previewBuilder.build().also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }

            // Image analysis for barcode scanning
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetResolution(android.util.Size(1280, 720))
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, BarcodeAnalyzer { barcodes, imageProxy ->
                        if (focusAchieved) {
                            processBarcodes(barcodes, imageProxy)
                        } // else ignore until focus is achieved
                    })
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
                Log.d(TAG, "Camera started successfully with continuous autofocus")
                startFocusMetering()
            } catch (exc: Exception) {
                Log.e(TAG, "Camera binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun startFocusMetering() {
        val viewFinder = binding.viewFinder
        val factory = viewFinder.meteringPointFactory
        val point = factory.createPoint(viewFinder.width / 2f, viewFinder.height / 2f)
        val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
            .setAutoCancelDuration(2, java.util.concurrent.TimeUnit.SECONDS)
            .build()
        isFocusing = true
        focusAchieved = false
        camera?.cameraControl?.startFocusAndMetering(action)?.addListener({
            isFocusing = false
            focusAchieved = true
            // Optionally hide focus indicator here
        }, ContextCompat.getMainExecutor(this))
        // Optionally show focus indicator (scan_overlay) while focusing
    }

    private fun processBarcodes(barcodes: List<Barcode>, imageProxy: ImageProxy? = null) {
        if (barcodes.isNotEmpty() && !isProcessingBarcode) {
            isProcessingBarcode = true
            val barcode = barcodes.first()
            barcode.rawValue?.let { value ->
                var format = getReadableFormat(barcode.format)
                Log.i(TAG, "Barcode detected: '$value' (Format: $format, Raw format: ${barcode.format})")
                if (format == "UNKNOWN" && imageProxy != null) {
                    // Try ZXing fallback
                    try {
                        val zxingResult = decodeWithZXing(imageProxy)
                        if (zxingResult != null) {
                            Log.i(TAG, "ZXing fallback succeeded: '${zxingResult.text}' (${zxingResult.barcodeFormat})")
                            processDetectedBarcode(barcode, zxingResult.text, zxingResult.barcodeFormat.toString())
                            imageProxy.close()
                            return
                        } else {
                            Log.w(TAG, "ZXing fallback failed. No barcode detected.")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "ZXing fallback error: ${e.message}", e)
                    }
                    // If ZXing also fails, show scan again dialog
                    runOnUiThread { showScanAgainDialog() }
                    imageProxy.close()
                    isProcessingBarcode = false
                    return
                }
                if (format == "UNKNOWN") {
                    runOnUiThread { showScanAgainDialog() }
                    isProcessingBarcode = false
                    return
                }
                processDetectedBarcode(barcode, value, format)
            } ?: run {
                Log.w(TAG, "Barcode detected but rawValue is null")
                isProcessingBarcode = false
            }
        }
    }

    private fun processDetectedBarcode(barcode: Barcode, value: String, format: String) {
        Log.d(TAG, "Processing barcode: value='$value', format='$format'")

        // Check if this is a product barcode that needs API lookup
        if (isProductBarcode(format)) {
            Log.d(TAG, "Product barcode detected - looking up product info")
            lookupProductInfo(value, format) { productName, brand, imageUrl ->
                Log.d(
                    TAG,
                    "⭐ Product lookup result: productName='$productName', brand='$brand', imageUrl='$imageUrl'"
                )
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

                // Improved null handling - we should always get a productInfo now
                // thanks to our fallback mechanism in BarcodeInfoService
                if (productInfo != null) {
                    val finalName = when {
                        productInfo.name.isNotEmpty() && productInfo.brand.isNotEmpty() ->
                            "${productInfo.brand} ${productInfo.name}"
                        productInfo.name.isNotEmpty() -> productInfo.name
                        productInfo.brand.isNotEmpty() -> productInfo.brand
                        else -> "Product: $barcodeValue" // Fallback name if somehow still empty
                    }

                    Log.i(
                        TAG,
                        "Product found: '$finalName', brand: '${productInfo.brand}', name: '${productInfo.name}', image: '${productInfo.imageUrl}'"
                    )

                    // CRITICAL LOG
                    Log.e(
                        "PRODUCT_DEBUG",
                        "FOUND PRODUCT: '$finalName', brand: '${productInfo.brand}', image: '${productInfo.imageUrl}'"
                    )

                    callback(finalName, productInfo.brand, productInfo.imageUrl)
                } else {
                    // This should almost never happen now, but just in case
                    Log.d(TAG, "No product info found - using default name")
                    val defaultName = "Product: $barcodeValue"
                    Log.e("PRODUCT_DEBUG", "USING DEFAULT: '$defaultName'")
                    callback(defaultName, "", "")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Product lookup failed: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    binding.loadingOverlay.visibility = View.GONE
                }
                // Even if we get an exception, still provide a default name
                val defaultName = "Product: $barcodeValue"
                Log.e("PRODUCT_DEBUG", "ERROR FALLBACK: '$defaultName'")
                callback(defaultName, "", "")
            }
        }
    }

    private fun getReadableFormat(format: Int): String {
        val formatName = when (format) {
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
            else -> {
                Log.w(TAG, "Unknown barcode format code: $format. This may be a new or unsupported format.")
                "UNKNOWN"
            }
        }
        Log.d(TAG, "Format conversion: raw=$format -> readable=$formatName")
        return formatName
    }

    private fun guessFormatFromValue(value: String): String {
        return when {
            // UPC-A: exactly 12 digits (sometimes stored as 11+check digit)
            value.matches(Regex("^\\d{12}$")) -> "UPC_A"
            value.matches(Regex("^\\d{11}$")) -> "UPC_A" // UPC-A without check digit

            // EAN-13: exactly 13 digits
            value.matches(Regex("^\\d{13}$")) -> "EAN_13"

            // EAN-8: exactly 8 digits
            value.matches(Regex("^\\d{8}$")) -> "EAN_8"

            // UPC-E: exactly 6 or 8 digits
            value.matches(Regex("^\\d{6}$")) -> "UPC_E"

            // ITF: even number of digits (Interleaved 2 of 5)
            value.matches(Regex("^\\d+$")) && value.length % 2 == 0 && value.length >= 4 -> "ITF"

            // CODE_39: alphanumeric with possible special characters
            value.matches(Regex("^[A-Z0-9\\-. \$/%+]+$")) -> "CODE_39"

            // QR Code patterns
            value.startsWith("http://") || value.startsWith("https://") -> "QR_CODE"
            value.startsWith("WIFI:") -> "QR_CODE"
            value.startsWith("BEGIN:VCARD") -> "QR_CODE"
            value.startsWith("geo:") -> "QR_CODE"
            value.contains("@") && value.contains(".") -> "QR_CODE" // Email-like

            // Default for everything else
            else -> "CODE_128"
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
        Log.i(
            TAG,
            "Finishing with barcode: '$value' ($format), productName='$productName', brand='$brand'"
        )

        val resultIntent = Intent().apply {
            putExtra(BARCODE_VALUE, value)
            putExtra(BARCODE_FORMAT, format)

            // Always include the product name extra, using a default if needed
            val finalProductName = if (!productName.isNullOrEmpty()) {
                Log.e("PRODUCT_DEBUG", "Sending back product name: '$productName'")
                productName
            } else if (isProductBarcode(format)) {
                val defaultName = "Product: $value"
                Log.e("PRODUCT_DEBUG", "Using default product name: '$defaultName'")
                defaultName
            } else {
                Log.e("PRODUCT_DEBUG", "No product name for non-product barcode")
                ""
            }

            // Always set these two extras
            putExtra(BARCODE_PRODUCT_NAME, finalProductName)
            putExtra(BARCODE_TITLE, finalProductName)

            if (!brand.isNullOrEmpty()) {
                Log.d(TAG, "Setting BARCODE_PRODUCT_BRAND to: '$brand'")
                putExtra(BARCODE_PRODUCT_BRAND, brand)
            }

            if (!imageUrl.isNullOrEmpty()) {
                Log.d(TAG, "Setting BARCODE_PRODUCT_IMAGE_URL to: '$imageUrl'")
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

    private fun decodeWithZXing(imageProxy: ImageProxy): Result? {
        val buffer = imageProxy.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val width = imageProxy.width
        val height = imageProxy.height
        val yuvImage = android.graphics.YuvImage(bytes, android.graphics.ImageFormat.NV21, width, height, null)
        val out = java.io.ByteArrayOutputStream()
        yuvImage.compressToJpeg(android.graphics.Rect(0, 0, width, height), 100, out)
        val jpegBytes = out.toByteArray()
        val bitmap = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
        val intArray = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(intArray, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val source: LuminanceSource = RGBLuminanceSource(bitmap.width, bitmap.height, intArray)
        val bitmapZX = BinaryBitmap(HybridBinarizer(source))
        return try {
            MultiFormatReader().decode(bitmapZX)
        } catch (e: NotFoundException) {
            null
        }
    }

    private fun showScanAgainDialog() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Barcode Not Recognized")
        builder.setMessage("The barcode could not be recognized. Please try again or enter it manually.")
        builder.setPositiveButton("Scan Again") { dialog, _ ->
            dialog.dismiss()
            isProcessingBarcode = false
        }
        builder.setNegativeButton("Manual Entry") { dialog, _ ->
            dialog.dismiss()
            finish() // Or launch manual entry activity/fragment
        }
        builder.setCancelable(false)
        builder.show()
    }

    private fun decodeWithZXingLegacy(imageProxy: ImageProxy): Result? {
        val buffer = imageProxy.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val width = imageProxy.width
        val height = imageProxy.height
        val yuvImage =
            android.graphics.YuvImage(bytes, android.graphics.ImageFormat.NV21, width, height, null)
        val out = java.io.ByteArrayOutputStream()
        yuvImage.compressToJpeg(android.graphics.Rect(0, 0, width, height), 100, out)
        val jpegBytes = out.toByteArray()
        val bitmap = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
        val intArray = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(intArray, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val source: LuminanceSource = RGBLuminanceSource(bitmap.width, bitmap.height, intArray)
        val bitmapZX = BinaryBitmap(HybridBinarizer(source))
        return try {
            MultiFormatReader().decode(bitmapZX)
        } catch (e: NotFoundException) {
            null
        }
    }

    private inner class BarcodeAnalyzer(private val onBarcodesDetected: (List<Barcode>, ImageProxy?) -> Unit) :
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
                            Log.d(TAG, "Found ${barcodes.size} barcode(s) in frame")
                        }
                        onBarcodesDetected(barcodes, imageProxy)
                    }
                    .addOnFailureListener { exception ->
                        Log.e(TAG, "Barcode scanning failed", exception)
                        isProcessingBarcode = false
                        imageProxy.close()
                    }
                    .addOnCompleteListener {
                        if (!isProcessingBarcode) imageProxy.close()
                    }
            } else {
                imageProxy.close()
            }
        }
    }
}
