package com.bar.honeypot

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import com.bar.honeypot.databinding.ActivityMainBinding
import com.bar.honeypot.util.VersionHelper
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Window
import android.app.Dialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.bar.honeypot.ui.gallery.GalleryListAdapter
import android.view.WindowManager
import android.widget.Button
import com.bar.honeypot.ui.gallery.GalleryItem

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val customGalleries = mutableListOf<String>()
    private lateinit var sharedPreferences: SharedPreferences
    private val PREFS_NAME = "HoneypotPrefs"
    private val GALLERIES_KEY = "savedGalleries"
    private lateinit var galleryAdapter: GalleryListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        
        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        // Load saved galleries
        loadSavedGalleries()

        setupRecyclerView()
        setupFab()
        
        // If there are no galleries, show the create gallery dialog
        if (customGalleries.isEmpty()) {
            showAddGalleryDialog()
        }
    }
    
    private fun loadSavedGalleries() {
        val savedGalleriesJson = sharedPreferences.getString(GALLERIES_KEY, null)
        if (!savedGalleriesJson.isNullOrEmpty()) {
            try {
                val type = object : TypeToken<List<String>>() {}.type
                val savedGalleries: List<String> = Gson().fromJson(savedGalleriesJson, type)
                customGalleries.clear()
                customGalleries.addAll(savedGalleries)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    private fun saveGalleries() {
        val editor = sharedPreferences.edit()
        val galleriesJson = Gson().toJson(customGalleries)
        editor.putString(GALLERIES_KEY, galleriesJson)
        editor.apply()
    }

    private fun setupRecyclerView() {
        galleryAdapter = GalleryListAdapter { galleryItem ->
            // Handle gallery item click
            // TODO: Navigate to gallery detail
            Toast.makeText(this, "Clicked: ${galleryItem.name}", Toast.LENGTH_SHORT).show()
        }
        
        binding.recyclerViewGalleries.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = galleryAdapter
        }

        // Load saved galleries into the RecyclerView
        val currentGalleries = customGalleries.map { name ->
            GalleryItem(name, "0 items")  // Initially set to 0 items
        }
        galleryAdapter.submitList(currentGalleries)
    }

    private fun setupFab() {
        binding.fabAddGallery.setOnClickListener {
            showAddGalleryDialog()
        }
    }
    
    private fun showAddGalleryDialog() {
        // Create a custom dialog
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_create_gallery)
        
        // Make dialog background transparent to show rounded corners
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        
        // Get references to dialog views
        val nameInput = dialog.findViewById<EditText>(R.id.edit_gallery_name)
        val cancelButton = dialog.findViewById<Button>(R.id.btn_create_gallery_cancel)
        val createButton = dialog.findViewById<Button>(R.id.btn_create_gallery_confirm)
        
        // Set click listeners
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }
        
        createButton.setOnClickListener {
            val galleryName = nameInput.text.toString().trim()
            
            when {
                galleryName.isEmpty() -> {
                    nameInput.error = "Gallery name cannot be empty"
                }
                customGalleries.contains(galleryName) -> {
                    nameInput.error = "Gallery with this name already exists"
                }
                else -> {
                    // Add new gallery
                    customGalleries.add(galleryName)
                    saveGalleries()
                    
                    // Update RecyclerView
                    val currentList = galleryAdapter.currentList.toMutableList()
                    currentList.add(GalleryItem(galleryName))
                    galleryAdapter.submitList(currentList)
                    
                    dialog.dismiss()
                }
            }
        }
        
        dialog.show()
    }
    
    private fun showDeleteGalleryDialog(galleryItem: GalleryItem) {
        // Create a custom dialog
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_delete_gallery)
        
        // Make dialog background transparent to show rounded corners
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        
        // Get references to dialog views
        val titleTextView = dialog.findViewById<TextView>(R.id.dialog_title)
        val messageTextView = dialog.findViewById<TextView>(R.id.dialog_message)
        val cancelButton = dialog.findViewById<Button>(R.id.btn_delete_gallery_cancel)
        val deleteButton = dialog.findViewById<Button>(R.id.btn_delete_gallery_confirm)
        
        // Set dialog text
        titleTextView.text = "Delete Gallery"
        messageTextView.text = "Are you sure you want to delete the gallery '${galleryItem.name}'? This action cannot be undone."
        
        // Set click listeners
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }
        
        deleteButton.setOnClickListener {
            // Remove gallery
            customGalleries.remove(galleryItem.name)
            saveGalleries()
            
            // Update RecyclerView
            val currentList = galleryAdapter.currentList.toMutableList()
            currentList.remove(galleryItem)
            galleryAdapter.submitList(currentList)
            
            dialog.dismiss()
        }
        
        dialog.show()
    }
}
