package com.bar.honeypot.ui.gallery

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bar.honeypot.databinding.FragmentGalleryBinding

class GalleryFragment : Fragment() {

    private var _binding: FragmentGalleryBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    
    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(context, "Camera permission is required to use the scanner", Toast.LENGTH_SHORT).show()
        }
    }
    
    private var galleryName: String = "Gallery"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val galleryViewModel =
            ViewModelProvider(this).get(GalleryViewModel::class.java)

        _binding = FragmentGalleryBinding.inflate(inflater, container, false)
        val root: View = binding.root
        
        // Get gallery name from arguments
        arguments?.let {
            if (it.containsKey("gallery_name")) {
                galleryName = it.getString("gallery_name", "Gallery")
            }
        }

        // Set the gallery name in the toolbar title
        (activity as? AppCompatActivity)?.supportActionBar?.title = galleryName
        
        // Set up the scanner FAB click listener
        binding.fabScanner.setOnClickListener {
            checkCameraPermissionAndOpenCamera()
        }
        
        return root
    }
    
    private fun checkCameraPermissionAndOpenCamera() {
        context?.let { ctx ->
            when {
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == 
                        PackageManager.PERMISSION_GRANTED -> {
                    // Permission is already granted, open camera
                    openCamera()
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
    
    private fun openCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            startActivity(takePictureIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Error opening camera: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}