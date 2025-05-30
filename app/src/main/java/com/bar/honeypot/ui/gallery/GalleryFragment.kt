package com.bar.honeypot.ui.gallery

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bar.honeypot.databinding.FragmentGalleryBinding
import com.bar.honeypot.ui.scanner.BarcodeScannerActivity

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
                galleryViewModel.addBarcode(barcodeValue, barcodeFormat)
                saveBarcodesToSharedPreferences()
                Toast.makeText(context, "Barcode scanned: $barcodeValue", Toast.LENGTH_SHORT).show()
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
    
    private fun setupRecyclerView() {
        barcodeAdapter = BarcodeAdapter()
        binding.recyclerViewBarcodes.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = barcodeAdapter
        }
        
        galleryViewModel.barcodes.observe(viewLifecycleOwner) { barcodes ->
            barcodeAdapter.submitList(barcodes)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}