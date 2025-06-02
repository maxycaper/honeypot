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
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.ImageView
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
            
            if (barcodeValue != null && barcodeFormat != null) {
                showBarcodeConfirmationDialog(barcodeValue, barcodeFormat)
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
        
        // Set up the RecyclerView
        setupRecyclerView()
        
        // Load saved barcodes for this gallery
        loadBarcodesFromSharedPreferences()
        
        return root
    }
    
    private fun showBarcodeConfirmationDialog(barcodeValue: String, barcodeFormat: String) {
        context?.let { ctx ->
            // Check if this barcode already exists in the gallery
            if (galleryViewModel.isDuplicateBarcode(barcodeValue)) {
                Toast.makeText(ctx, "This barcode already exists in '${galleryViewModel.currentGalleryName.value}'", Toast.LENGTH_LONG).show()
                return
            }
            
            // Create a custom dialog
            val dialog = Dialog(ctx)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(R.layout.dialog_barcode_confirmation)
            
            // Make dialog background transparent to show rounded corners
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            
            // Set up dialog views
            val valueTextView = dialog.findViewById<TextView>(R.id.barcode_value)
            val formatTextView = dialog.findViewById<TextView>(R.id.barcode_format)
            val cancelButton = dialog.findViewById<Button>(R.id.btn_cancel)
            val saveButton = dialog.findViewById<Button>(R.id.btn_save)
            
            // Set barcode information
            valueTextView.text = barcodeValue
            formatTextView.text = "Format: $barcodeFormat"
            
            // Set button listeners
            cancelButton.setOnClickListener {
                dialog.dismiss()
            }
            
            saveButton.setOnClickListener {
                // Add the barcode (no need to check for duplicates again here)
                galleryViewModel.addBarcode(barcodeValue, barcodeFormat)
                saveBarcodesToSharedPreferences()
                Toast.makeText(ctx, "Barcode saved to gallery", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            
            dialog.show()
        }
    }
    
    private fun setupRecyclerView() {
        barcodeAdapter = BarcodeAdapter { barcode ->
            showBarcodeDisplayDialog(barcode)
        }
        binding.recyclerViewBarcodes.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = barcodeAdapter
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
            val cancelButton = dialog.findViewById<Button>(R.id.btn_cancel)
            val saveButton = dialog.findViewById<Button>(R.id.btn_save)
            
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

    private fun showBarcodeDisplayDialog(barcode: BarcodeData) {
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
            val closeButton = dialog.findViewById<Button>(R.id.btn_close)
            
            // Set barcode information
            titleTextView.text = "Barcode Display"
            valueTextView.text = barcode.value
            formatTextView.text = "Format: ${barcode.format}"
            
            // Generate and display the barcode
            generateBarcodeImage(barcode.value, barcode.format, barcodeImageView)
            
            // Adjust layout for long barcodes if needed
            if (isLongCode128) {
                barcodeImageView.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                barcodeImageView.layoutParams.height = 200
                barcodeImageView.layoutParams = barcodeImageView.layoutParams
            }
            
            // Set button listener
            closeButton.setOnClickListener {
                dialog.dismiss()
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