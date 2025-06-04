package com.bar.honeypot.ui.gallery

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bar.honeypot.R
import com.bar.honeypot.databinding.FragmentGalleryBinding
import com.bar.honeypot.model.BarcodeData
import com.bar.honeypot.ui.scanner.BarcodeScannerActivity
import com.google.android.material.snackbar.Snackbar

class GalleryFragment : Fragment() {

    private var _binding: FragmentGalleryBinding? = null
    private lateinit var galleryViewModel: GalleryViewModel
    private lateinit var barcodeAdapter: BarcodeAdapter

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    
    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            launchBarcodeScanner()
        } else {
            Toast.makeText(context, "Camera permission is required to use the scanner", Toast.LENGTH_SHORT).show()
        }
    }
    
    private val barcodeScannerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val barcodeValue = data?.getStringExtra(BarcodeScannerActivity.BARCODE_VALUE)
            val barcodeFormat = data?.getStringExtra(BarcodeScannerActivity.BARCODE_FORMAT)
            val productName = data?.getStringExtra(BarcodeScannerActivity.BARCODE_PRODUCT_NAME)
            val productBrand = data?.getStringExtra(BarcodeScannerActivity.BARCODE_PRODUCT_BRAND)
            val productImageUrl = data?.getStringExtra(BarcodeScannerActivity.BARCODE_PRODUCT_IMAGE_URL)
            val title = data?.getStringExtra(BarcodeScannerActivity.BARCODE_TITLE)
            
            // Log all extras from the intent for debugging
            Log.d("GalleryFragment", "--- Barcode Scanner Result Extras ---")
            data?.extras?.keySet()?.forEach { key ->
                Log.d("GalleryFragment", "Extra: $key = ${data.extras?.get(key)}")
            }
            
            // Log received product information
            Log.d("GalleryFragment", "Received barcode: $barcodeValue, format: $barcodeFormat")
            Log.d("GalleryFragment", "Product info - name: '$productName', brand: '$productBrand', title: '$title', imageUrl: '$productImageUrl'")
            
            if (barcodeValue != null && barcodeFormat != null) {
                showBarcodeConfirmationDialog(barcodeValue, barcodeFormat, data)
            }
        }
    }
    
    private var galleryName: String = "Gallery"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        galleryViewModel = ViewModelProvider(this).get(GalleryViewModel::class.java)

        _binding = FragmentGalleryBinding.inflate(inflater, container, false)
        val root: View = binding.root
        
        // Get gallery name from arguments
        arguments?.let {
            if (it.containsKey("gallery_name")) {
                galleryName = it.getString("gallery_name", "Gallery")
                galleryViewModel.setGalleryName(galleryName)
            }
        }

        // Set the gallery name in the toolbar title
        (activity as? AppCompatActivity)?.supportActionBar?.title = galleryName
        
        // Set up the scanner FAB click listener
        binding.fabScanner.setOnClickListener {
            checkCameraPermissionAndScan()
        }
        
        // Set up the manual entry FAB click listener
        binding.fabManualEntry.setOnClickListener {
            showManualBarcodeEntryDialog()
        }
        
        // Set up the RecyclerView
        setupRecyclerView()
        
        // Load saved barcodes for this gallery
        loadBarcodesFromSharedPreferences()
        
        return root
    }
    
    private fun showBarcodeConfirmationDialog(barcodeValue: String, barcodeFormat: String, data: Intent) {
        context?.let { ctx ->
            // Check if this barcode already exists in the gallery
            if (galleryViewModel.isDuplicateBarcode(barcodeValue)) {
                Toast.makeText(ctx, "This barcode already exists in '${galleryViewModel.currentGalleryName.value}'", Toast.LENGTH_LONG).show()
                return
            }
            
            // Get additional metadata from intent
            val title = data.getStringExtra(BarcodeScannerActivity.BARCODE_TITLE) ?: ""
            val description = data.getStringExtra(BarcodeScannerActivity.BARCODE_DESCRIPTION) ?: ""
            val url = data.getStringExtra(BarcodeScannerActivity.BARCODE_URL) ?: ""
            val email = data.getStringExtra(BarcodeScannerActivity.BARCODE_EMAIL) ?: ""
            val phone = data.getStringExtra(BarcodeScannerActivity.BARCODE_PHONE) ?: ""
            val smsContent = data.getStringExtra(BarcodeScannerActivity.BARCODE_SMS) ?: ""
            val wifiSsid = data.getStringExtra(BarcodeScannerActivity.BARCODE_WIFI_SSID) ?: ""
            val wifiPassword = data.getStringExtra(BarcodeScannerActivity.BARCODE_WIFI_PASSWORD) ?: ""
            val wifiType = data.getStringExtra(BarcodeScannerActivity.BARCODE_WIFI_TYPE) ?: ""
            val geoLat = data.getDoubleExtra(BarcodeScannerActivity.BARCODE_GEO_LAT, 0.0)
            val geoLng = data.getDoubleExtra(BarcodeScannerActivity.BARCODE_GEO_LNG, 0.0)
            val productName = data.getStringExtra(BarcodeScannerActivity.BARCODE_PRODUCT_NAME) ?: ""
            val contactInfo = data.getStringExtra(BarcodeScannerActivity.BARCODE_CONTACT_INFO) ?: ""
            val productImageUrl = data.getStringExtra(BarcodeScannerActivity.BARCODE_PRODUCT_IMAGE_URL) ?: ""
            
            // Create a custom dialog
            val dialog = Dialog(ctx)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(R.layout.dialog_barcode_confirmation)
            
            // Make dialog background transparent to show rounded corners
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            
            // Set up dialog views
            val dialogTitle = dialog.findViewById<TextView>(R.id.dialog_title)
            val valueTextView = dialog.findViewById<TextView>(R.id.barcode_value)
            val formatTextView = dialog.findViewById<TextView>(R.id.barcode_format)
            val descriptionView = dialog.findViewById<TextView>(R.id.barcode_description)
            val productImageView = dialog.findViewById<ImageView>(R.id.product_image)
            val cancelButton = dialog.findViewById<Button>(R.id.btn_barcode_cancel)
            val saveButton = dialog.findViewById<Button>(R.id.btn_barcode_save)
            
            // Set informative title based on content
            val dialogTitleText = when {
                productName.isNotEmpty() && !productName.startsWith("Product:") -> productName
                title.isNotEmpty() -> title
                url.isNotEmpty() -> "Save URL Barcode?"
                email.isNotEmpty() -> "Save Email Barcode?"
                phone.isNotEmpty() -> "Save Phone Barcode?"
                wifiSsid.isNotEmpty() -> "Save WiFi Network?"
                contactInfo.isNotEmpty() -> "Save Contact Information?"
                geoLat != 0.0 && geoLng != 0.0 -> "Save Location?"
                else -> "Save Barcode?"
            }
            dialogTitle.text = dialogTitleText
            
            // Set barcode information
            valueTextView.text = barcodeValue
            
            // Enhanced format display
            val formatText = StringBuilder("Format: $barcodeFormat")
            
            // Add metadata type if available
            if (url.isNotEmpty()) {
                formatText.append(" (URL)")
            } else if (email.isNotEmpty()) {
                formatText.append(" (Email)")
            } else if (phone.isNotEmpty()) {
                formatText.append(" (Phone)")
            } else if (wifiSsid.isNotEmpty()) {
                formatText.append(" (WiFi)")
            } else if (contactInfo.isNotEmpty()) {
                formatText.append(" (Contact)")
            } else if (productName.isNotEmpty()) {
                formatText.append(" (Product)")
            }
            
            formatTextView.text = formatText.toString()
            
            // Show product image if available
            if (productImageUrl.isNotEmpty()) {
                // Load the image using Glide or similar library
                // For this example, we'll just show the image view and set a background color
                productImageView.visibility = View.VISIBLE
                // You would use a library like Glide or Picasso here to load the image:
                // Glide.with(this).load(productImageUrl).into(productImageView)
            } else {
                productImageView.visibility = View.GONE
            }
            
            // Show all available metadata in a formatted way
            val metadataText = StringBuilder()
            
            if (productName.isNotEmpty() && !dialogTitleText.equals(productName)) {
                if (productName.startsWith("Product:")) {
                    metadataText.append("<b>Barcode:</b> ${productName.substringAfter("Product: ")}").append("<br><br>")
                } else {
                    metadataText.append("<b>Product:</b> ${productName}").append("<br><br>")
                }
            }
            
            if (description.isNotEmpty()) {
                metadataText.append("<b>Description:</b> ${description}").append("<br><br>")
            }
            
            if (url.isNotEmpty()) {
                metadataText.append("<b>URL:</b> ${url}").append("<br><br>")
            }
            
            if (email.isNotEmpty()) {
                metadataText.append("<b>Email:</b> ${email}").append("<br><br>")
            }
            
            if (phone.isNotEmpty()) {
                metadataText.append("<b>Phone:</b> ${phone}").append("<br><br>")
            }
            
            if (smsContent.isNotEmpty()) {
                metadataText.append("<b>SMS:</b> ${smsContent}").append("<br><br>")
            }
            
            if (wifiSsid.isNotEmpty()) {
                metadataText.append("<b>WiFi SSID:</b> ${wifiSsid}")
                if (wifiPassword.isNotEmpty()) {
                    metadataText.append("<br><b>Password:</b> ${wifiPassword}")
                }
                if (wifiType.isNotEmpty()) {
                    metadataText.append("<br><b>Type:</b> ${wifiType}")
                }
                metadataText.append("<br><br>")
            }
            
            if (contactInfo.isNotEmpty()) {
                metadataText.append("<b>Contact Info:</b><br>${contactInfo.replace("\n", "<br>")}").append("<br><br>")
            }
            
            if (geoLat != 0.0 && geoLng != 0.0) {
                metadataText.append("<b>Location:</b> ${geoLat}, ${geoLng}").append("<br><br>")
            }
            
            // Set description text or hide if none
            if (metadataText.isNotEmpty()) {
                descriptionView.text = Html.fromHtml(metadataText.toString().trim(), Html.FROM_HTML_MODE_COMPACT)
                descriptionView.visibility = View.VISIBLE
            } else {
                descriptionView.visibility = View.GONE
            }
            
            // Set button listeners
            cancelButton.setOnClickListener {
                dialog.dismiss()
            }
            
            saveButton.setOnClickListener {
                // Add the barcode with all metadata
                galleryViewModel.addBarcode(
                    value = barcodeValue,
                    format = barcodeFormat,
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
                saveBarcodesToSharedPreferences()
                Toast.makeText(ctx, "Barcode saved to gallery", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            
            dialog.show()
        }
    }
    
    private fun showManualBarcodeEntryDialog() {
        context?.let { ctx ->
            // Create a custom dialog
            val dialog = Dialog(ctx)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(R.layout.dialog_manual_barcode_entry)
            
            // Make dialog background transparent to show rounded corners
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            
            // Set up dialog views
            val barcodeValueField = dialog.findViewById<EditText>(R.id.edit_barcode_value)
            val barcodeFormatSpinner = dialog.findViewById<Spinner>(R.id.spinner_barcode_format)
            val barcodeTitleField = dialog.findViewById<EditText>(R.id.edit_barcode_title)
            val cancelButton = dialog.findViewById<Button>(R.id.btn_manual_entry_cancel)
            val addButton = dialog.findViewById<Button>(R.id.btn_manual_entry_add)
            
            // Set up the format spinner
            val barcodeFormats = arrayOf(
                "QR_CODE",
                "CODE_128", 
                "CODE_39",
                "CODE_93",
                "EAN_13",
                "EAN_8",
                "UPC_A",
                "UPC_E",
                "DATA_MATRIX",
                "AZTEC",
                "PDF417",
                "CODABAR",
                "ITF"
            )
            
            val adapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_item, barcodeFormats)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            barcodeFormatSpinner.adapter = adapter
            
            // Set QR_CODE as default (position 0)
            barcodeFormatSpinner.setSelection(0)
            
            // Set button listeners
            cancelButton.setOnClickListener {
                dialog.dismiss()
            }
            
            addButton.setOnClickListener {
                val barcodeValue = barcodeValueField.text.toString().trim()
                val barcodeFormat = barcodeFormatSpinner.selectedItem.toString()
                val barcodeTitle = barcodeTitleField.text.toString().trim()
                
                if (barcodeValue.isEmpty()) {
                    Toast.makeText(ctx, "Please enter a barcode value", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                
                // Check if this barcode already exists in the gallery
                if (galleryViewModel.isDuplicateBarcode(barcodeValue)) {
                    Toast.makeText(ctx, "This barcode already exists in '${galleryViewModel.currentGalleryName.value}'", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                
                // Add the barcode manually
                val added = galleryViewModel.addBarcode(
                    value = barcodeValue,
                    format = barcodeFormat,
                    title = barcodeTitle
                )
                
                if (added) {
                    saveBarcodesToSharedPreferences()
                    Toast.makeText(ctx, "Barcode added to gallery", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                } else {
                    Toast.makeText(ctx, "Failed to add barcode", Toast.LENGTH_SHORT).show()
                }
            }
            
            dialog.show()
        }
    }
    
    private fun setupRecyclerView() {
        barcodeAdapter = BarcodeAdapter { barcode, position ->
            showBarcodeDisplayDialog(barcode, position)
        }
        binding.recyclerViewBarcodes.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = barcodeAdapter
            
            // Add spacing between items
            addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    // Add bottom margin to each item
                    outRect.bottom = resources.getDimensionPixelSize(R.dimen.barcode_item_spacing)
                }
            })
        }
        
        // Add swipe-to-delete functionality
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false // We don't support moving items
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val deletedBarcode = galleryViewModel.barcodes.value?.get(position)
                
                // Notify adapter to reset the view (cancel the swipe)
                barcodeAdapter.notifyItemChanged(position)
                
                // Show confirmation dialog
                deletedBarcode?.let { barcode ->
                    showDeleteConfirmationDialog(barcode, position)
                }
            }
            
            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                val background = ColorDrawable()
                
                // Set red background when swiping
                background.color = ContextCompat.getColor(requireContext(), android.R.color.holo_red_light)
                
                if (dX > 0) { // Swiping to the right
                    background.setBounds(
                        itemView.left,
                        itemView.top,
                        itemView.left + dX.toInt(),
                        itemView.bottom
                    )
                } else { // Swiping to the left
                    background.setBounds(
                        itemView.right + dX.toInt(),
                        itemView.top,
                        itemView.right,
                        itemView.bottom
                    )
                }
                
                background.draw(c)
                
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        })
        
        itemTouchHelper.attachToRecyclerView(binding.recyclerViewBarcodes)
        
        galleryViewModel.barcodes.observe(viewLifecycleOwner) { barcodes ->
            barcodeAdapter.submitList(barcodes)
            
            // Show/hide empty view
            binding.emptyView.visibility = if (barcodes.isEmpty()) View.VISIBLE else View.GONE
        }
    }
    
    private fun checkCameraPermissionAndScan() {
        context?.let { ctx ->
            when {
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == 
                        PackageManager.PERMISSION_GRANTED -> {
                    // Permission is already granted, launch barcode scanner
                    launchBarcodeScanner()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                    // Explain why we need the permission
                    Toast.makeText(ctx, "Camera permission is needed to use the scanner", Toast.LENGTH_SHORT).show()
                    requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
                else -> {
                    // Request the permission
                    requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }
        }
    }
    
    private fun launchBarcodeScanner() {
        val intent = Intent(requireContext(), BarcodeScannerActivity::class.java)
        barcodeScannerLauncher.launch(intent)
    }
    
    private fun saveBarcodesToSharedPreferences() {
        galleryViewModel.saveBarcodes { barcodesJson ->
            val sharedPreferences = requireActivity().getSharedPreferences("HoneypotPrefs", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putString("barcodes_$galleryName", barcodesJson)
            editor.apply()
        }
    }
    
    private fun loadBarcodesFromSharedPreferences() {
        val sharedPreferences = requireActivity().getSharedPreferences("HoneypotPrefs", Context.MODE_PRIVATE)
        val barcodesJson = sharedPreferences.getString("barcodes_$galleryName", null)
        galleryViewModel.loadBarcodes(galleryName, barcodesJson)
    }

    private fun showDeleteConfirmationDialog(barcode: BarcodeData, position: Int) {
        context?.let { ctx ->
            // Create a custom dialog
            val dialog = Dialog(ctx)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(R.layout.dialog_barcode_confirmation)
            
            // Make dialog background transparent to show rounded corners
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            
            // Set up dialog views
            val titleTextView = dialog.findViewById<TextView>(R.id.dialog_title)
            val valueTextView = dialog.findViewById<TextView>(R.id.barcode_value)
            val formatTextView = dialog.findViewById<TextView>(R.id.barcode_format)
            val cancelButton = dialog.findViewById<Button>(R.id.btn_barcode_cancel)
            val saveButton = dialog.findViewById<Button>(R.id.btn_barcode_save)
            
            // Customize dialog for deletion
            titleTextView.text = "Delete Barcode?"
            valueTextView.text = barcode.value
            formatTextView.text = "Format: ${barcode.format}"
            cancelButton.text = "Cancel"
            saveButton.text = "Delete"
            
            // Set button listeners
            cancelButton.setOnClickListener {
                dialog.dismiss()
            }
            
            saveButton.setOnClickListener {
                // Delete the barcode
                galleryViewModel.deleteBarcode(position)
                saveBarcodesToSharedPreferences()
                
                // Show undo option
                val snackbar = Snackbar.make(
                    binding.root,
                    "Barcode deleted from '${galleryViewModel.currentGalleryName.value}'",
                    Snackbar.LENGTH_LONG
                )
                snackbar.setAction("UNDO") {
                    // Add the barcode back
                    val added = galleryViewModel.addBarcode(barcode.value, barcode.format)
                    if (added) {
                        saveBarcodesToSharedPreferences()
                    }
                }
                snackbar.show()
                
                dialog.dismiss()
            }
            
            dialog.show()
        }
    }

    private fun showBarcodeDisplayDialog(barcode: BarcodeData, position: Int) {
        context?.let { ctx ->
            // Create a custom dialog
            val dialog = Dialog(ctx)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(R.layout.dialog_display_barcode)
            
            // Make dialog background transparent to show rounded corners
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            
            // Check if this is a long CODE_128 barcode
            val isLongCode128 = barcode.format == "CODE_128" && barcode.value.length > 20
            
            // Set up dialog views
            val titleTextView = dialog.findViewById<TextView>(R.id.dialog_title)
            val valueTextView = dialog.findViewById<TextView>(R.id.barcode_value)
            val formatTextView = dialog.findViewById<TextView>(R.id.barcode_format)
            val barcodeImageView = dialog.findViewById<ImageView>(R.id.barcode_image)
            val productImageView = dialog.findViewById<ImageView>(R.id.product_image)
            val descriptionView = dialog.findViewById<TextView>(R.id.barcode_description)
            val closeButton = dialog.findViewById<Button>(R.id.btn_display_close)
            val editTitleButton = dialog.findViewById<Button>(R.id.btn_display_edit_title)
            
            // Set informative title based on content
            val title = when {
                barcode.productName.isNotEmpty() && !barcode.productName.startsWith("Product:") -> barcode.productName
                barcode.title.isNotEmpty() -> barcode.title
                barcode.url.isNotEmpty() -> "URL Barcode"
                barcode.email.isNotEmpty() -> "Email Barcode"
                barcode.phone.isNotEmpty() -> "Phone Barcode"
                barcode.wifiSsid.isNotEmpty() -> "WiFi Network"
                barcode.contactInfo.isNotEmpty() -> "Contact Information"
                barcode.geoLat != 0.0 && barcode.geoLng != 0.0 -> "Location"
                else -> "Barcode Display"
            }
            titleTextView.text = title
            
            // Show/hide edit title button based on whether a custom title can be useful
            val needsCustomTitle = barcode.productName.isEmpty() || barcode.productName.startsWith("Product:")
            editTitleButton.visibility = if (needsCustomTitle) View.VISIBLE else View.GONE
            
            valueTextView.text = barcode.value
            
            // Enhanced format display
            val formatText = StringBuilder("Format: ${barcode.format}")
            
            // Add metadata type if barcode has additional data
            if (barcode.hasMetadata()) {
                formatText.append(" (${barcode.getMetadataType()})")
            }
            
            formatTextView.text = formatText.toString()
            
            // Show product image if available
            if (barcode.productImageUrl.isNotEmpty()) {
                productImageView.visibility = View.VISIBLE
                // You would use a library like Glide or Picasso here to load the image:
                // Glide.with(this).load(barcode.productImageUrl).into(productImageView)
            } else {
                productImageView.visibility = View.GONE
            }
            
            // Generate and display the barcode
            generateBarcodeImage(barcode.value, barcode.format, barcodeImageView)
            
            // Adjust layout for long barcodes if needed
            if (isLongCode128) {
                barcodeImageView.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                barcodeImageView.layoutParams.height = 200
                barcodeImageView.layoutParams = barcodeImageView.layoutParams
            }
            
            // Show all available metadata in a formatted way
            val metadataText = StringBuilder()
            
            if (barcode.productName.isNotEmpty() && !title.equals(barcode.productName)) {
                if (barcode.productName.startsWith("Product:")) {
                    metadataText.append("<b>Barcode:</b> ${barcode.productName.substringAfter("Product: ")}").append("<br><br>")
                } else {
                    metadataText.append("<b>Product:</b> ${barcode.productName}").append("<br><br>")
                }
            }

            if (barcode.description.isNotEmpty()) {
                metadataText.append("<b>Description:</b> ${barcode.description}").append("<br><br>")
            }
            
            if (barcode.url.isNotEmpty()) {
                metadataText.append("<b>URL:</b> ${barcode.url}").append("<br><br>")
            }
            
            if (barcode.email.isNotEmpty()) {
                metadataText.append("<b>Email:</b> ${barcode.email}").append("<br><br>")
            }
            
            if (barcode.phone.isNotEmpty()) {
                metadataText.append("<b>Phone:</b> ${barcode.phone}").append("<br><br>")
            }
            
            if (barcode.smsContent.isNotEmpty()) {
                metadataText.append("<b>SMS:</b> ${barcode.smsContent}").append("<br><br>")
            }
            
            if (barcode.wifiSsid.isNotEmpty()) {
                metadataText.append("<b>WiFi SSID:</b> ${barcode.wifiSsid}")
                if (barcode.wifiPassword.isNotEmpty()) {
                    metadataText.append("<br><b>Password:</b> ${barcode.wifiPassword}")
                }
                if (barcode.wifiType.isNotEmpty()) {
                    metadataText.append("<br><b>Type:</b> ${barcode.wifiType}")
                }
                metadataText.append("<br><br>")
            }
            
            if (barcode.contactInfo.isNotEmpty()) {
                metadataText.append("<b>Contact Info:</b><br>${barcode.contactInfo.replace("\n", "<br>")}").append("<br><br>")
            }
            
            if (barcode.geoLat != 0.0 && barcode.geoLng != 0.0) {
                metadataText.append("<b>Location:</b> ${barcode.geoLat}, ${barcode.geoLng}").append("<br><br>")
            }
            
            // Set description text or hide if none
            if (metadataText.isNotEmpty()) {
                descriptionView.text = Html.fromHtml(metadataText.toString().trim(), Html.FROM_HTML_MODE_COMPACT)
                descriptionView.visibility = View.VISIBLE
            } else {
                descriptionView.visibility = View.GONE
            }
            
            // Set button listeners
            closeButton.setOnClickListener {
                dialog.dismiss()
            }
            
            editTitleButton.setOnClickListener {
                dialog.dismiss()
                showEditTitleDialog(barcode, position)
            }
            
            dialog.show()
        }
    }
    
    private fun showEditTitleDialog(barcode: BarcodeData, position: Int) {
        context?.let { ctx ->
            // Create a custom dialog
            val dialog = Dialog(ctx)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(R.layout.dialog_edit_barcode_title)
            
            // Make dialog background transparent to show rounded corners
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            
            // Set up dialog views
            val editTitleField = dialog.findViewById<EditText>(R.id.edit_title)
            val valueTextView = dialog.findViewById<TextView>(R.id.barcode_value)
            val formatTextView = dialog.findViewById<TextView>(R.id.barcode_format)
            val cancelButton = dialog.findViewById<Button>(R.id.btn_edit_title_cancel)
            val saveButton = dialog.findViewById<Button>(R.id.btn_edit_title_save)
            
            // Pre-fill with existing title if any
            editTitleField.setText(barcode.title)
            
            // Set barcode information
            valueTextView.text = barcode.value
            formatTextView.text = "Format: ${barcode.format}"
            
            // Set button listeners
            cancelButton.setOnClickListener {
                dialog.dismiss()
            }
            
            saveButton.setOnClickListener {
                val newTitle = editTitleField.text.toString().trim()
                if (newTitle.isNotEmpty()) {
                    // Update the barcode title
                    if (galleryViewModel.updateBarcodeTitle(position, newTitle)) {
                        // Save the updated barcode data
                        saveBarcodesToSharedPreferences()
                        Toast.makeText(ctx, "Title updated", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(ctx, "Title cannot be empty", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                dialog.dismiss()
                
                // Show the display dialog again with updated information
                showBarcodeDisplayDialog(galleryViewModel.barcodes.value!![position], position)
            }
            
            dialog.show()
        }
    }
    
    private fun generateBarcodeImage(barcodeValue: String, barcodeFormat: String, imageView: ImageView) {
        try {
            // Determine the barcode format to use
            val format = when (barcodeFormat) {
                "QR_CODE" -> com.google.zxing.BarcodeFormat.QR_CODE
                "CODE_128" -> com.google.zxing.BarcodeFormat.CODE_128
                "CODE_39" -> com.google.zxing.BarcodeFormat.CODE_39
                "CODE_93" -> com.google.zxing.BarcodeFormat.CODE_93
                "EAN_13" -> com.google.zxing.BarcodeFormat.EAN_13
                "EAN_8" -> com.google.zxing.BarcodeFormat.EAN_8
                "UPC_A" -> com.google.zxing.BarcodeFormat.UPC_A
                "UPC_E" -> com.google.zxing.BarcodeFormat.UPC_E
                "DATA_MATRIX" -> com.google.zxing.BarcodeFormat.DATA_MATRIX
                "AZTEC" -> com.google.zxing.BarcodeFormat.AZTEC
                "PDF417" -> com.google.zxing.BarcodeFormat.PDF_417
                "CODABAR" -> com.google.zxing.BarcodeFormat.CODABAR
                "ITF" -> com.google.zxing.BarcodeFormat.ITF
                else -> com.google.zxing.BarcodeFormat.QR_CODE // Default to QR code
            }
            
            // Create MultiFormatWriter to generate the barcode
            val writer = com.journeyapps.barcodescanner.BarcodeEncoder()
            
            // Adjust width for CODE_128
            val width = if (barcodeFormat == "CODE_128" && barcodeValue.length > 20) 800 else 512
            val height = if (barcodeFormat == "CODE_128" && barcodeValue.length > 20) 200 else 512
            
            val bitMatrix = writer.encode(barcodeValue, format, width, height)
            val bitmap = writer.createBitmap(bitMatrix)
            
            // Set the bitmap to the ImageView
            imageView.setImageBitmap(bitmap)
        } catch (e: Exception) {
            // If there's an error generating the barcode, show an error message
            Toast.makeText(context, "Error generating barcode: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}