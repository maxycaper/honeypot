package com.bar.honeypot

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.WindowCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.bar.honeypot.databinding.ActivityMainBinding
import com.bar.honeypot.model.BarcodeData
import com.bar.honeypot.ui.gallery.GalleryDetailFragment
import com.bar.honeypot.ui.gallery.GalleryItem
import com.bar.honeypot.ui.gallery.GalleryListAdapter
import com.bar.honeypot.util.VersionHelper
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import android.app.Dialog
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val customGalleries = ArrayList<String>()
    private lateinit var sharedPreferences: SharedPreferences
    private val PREFS_NAME = "HoneypotPrefs"
    private val GALLERIES_KEY = "savedGalleries"
    private lateinit var galleryAdapter: GalleryListAdapter

    // Register the file picker for import
    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val scanner = Scanner(inputStream)
                    val text = scanner.useDelimiter("\\A").next()
                    scanner.close()
                    inputStream.close()
                    importGalleries(text)
                }
            } catch (e: IOException) {
                Toast.makeText(this, "Failed to import file: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Register the file picker for export
    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val outputStream = contentResolver.openOutputStream(uri)
                if (outputStream != null) {
                    val exportedData = exportGalleries()
                    outputStream.write(exportedData.toByteArray())
                    outputStream.close()
                    Toast.makeText(this, "Export successful!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: IOException) {
                Toast.makeText(this, "Failed to export file: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge layout
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        // Add top margin to toolbar and toolbar background to account for status bar
        // Also add bottom padding to RecyclerView and bottom margin to FAB to avoid overlap with navigation bar
        binding.root.setOnApplyWindowInsetsListener { view, insets ->
            val statusBarHeight = insets.systemWindowInsetTop
            val navigationBarHeight = insets.systemWindowInsetBottom

            // Add top margin to toolbar background
            val toolbarBgParams =
                binding.toolbarBackground.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            toolbarBgParams.topMargin = statusBarHeight
            binding.toolbarBackground.layoutParams = toolbarBgParams

            // Add top margin to toolbar
            val toolbarParams =
                binding.toolbar.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            toolbarParams.topMargin = statusBarHeight
            binding.toolbar.layoutParams = toolbarParams

            // Add bottom padding to RecyclerView to avoid overlap with navigation bar
            val recyclerViewParams =
                binding.recyclerViewGalleries.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            binding.recyclerViewGalleries.setPadding(
                binding.recyclerViewGalleries.paddingLeft,
                binding.recyclerViewGalleries.paddingTop,
                binding.recyclerViewGalleries.paddingRight,
                88 + navigationBarHeight // 88dp for FAB space + navigation bar height
            )

            // Add bottom margin to FAB to avoid overlap with navigation bar
            val fabParams =
                binding.fabAddGallery.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            fabParams.bottomMargin =
                16 + navigationBarHeight // Original margin + navigation bar height
            binding.fabAddGallery.layoutParams = fabParams

            insets
        }

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Load saved galleries
        loadSavedGalleries()

        setupRecyclerView()
        setupFab()

        // Setup back stack listener
        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0) {
                // We've returned to the main screen, restore visibility
                binding.recyclerViewGalleries.visibility = View.VISIBLE
                binding.fabAddGallery.visibility = View.VISIBLE
            }
        }

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
        galleryAdapter = GalleryListAdapter(
            onItemClick = { galleryItem ->
                // Handle gallery item click - navigate to detail view
                navigateToGalleryDetail(galleryItem.name)
            },
            onArrowClick = { galleryItem ->
                // Navigate to gallery detail view
                navigateToGalleryDetail(galleryItem.name)
            }
        )

        binding.recyclerViewGalleries.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = galleryAdapter
        }

        // Add swipe to delete functionality
        val itemTouchHelper =
            ItemTouchHelper(object :
                ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    return false
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val position = viewHolder.adapterPosition
                    val galleryItem = galleryAdapter.currentList[position]
                    // Restore the item to its original state (cancel the swipe)
                    galleryAdapter.notifyItemChanged(position)
                    if (direction == ItemTouchHelper.LEFT) {
                        // Show the delete dialog
                        showDeleteGalleryDialog(galleryItem)
                    } else if (direction == ItemTouchHelper.RIGHT) {
                        // Show the edit dialog
                        showEditGalleryDialog(galleryItem)
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
                    val icon: Drawable?

                    // Different visuals based on swipe direction
                    if (dX > 0) { // Swiping right (edit)
                        background.color =
                            ContextCompat.getColor(this@MainActivity, R.color.neon_hot_pink)
                        icon = ContextCompat.getDrawable(this@MainActivity, R.drawable.ic_edit)

                        // Set background
                        background.setBounds(
                            itemView.left,
                            itemView.top,
                            itemView.left + dX.toInt(),
                            itemView.bottom
                        )

                        // Draw icon if swiped enough
                        if (dX > 20) {
                            icon?.setBounds(
                                itemView.left + 50,
                                itemView.top + (itemView.bottom - itemView.top - icon.intrinsicHeight) / 2,
                                itemView.left + 80 + icon.intrinsicWidth,
                                itemView.top + (itemView.bottom - itemView.top) / 2 + icon.intrinsicHeight
                            )
                        }
                    } else { // Swiping left (delete)
                        background.color = Color.RED
                        icon = ContextCompat.getDrawable(this@MainActivity, R.drawable.ic_delete)

                        // Set background
                        background.setBounds(
                            itemView.right + dX.toInt(),
                            itemView.top,
                            itemView.right,
                            itemView.bottom
                        )

                        // Draw icon if swiped enough
                        if (dX < -20) {
                            icon?.setBounds(
                                itemView.right - 80,
                                itemView.top + (itemView.bottom - itemView.top - icon.intrinsicHeight) / 2,
                                itemView.right - 50,
                                itemView.top + (itemView.bottom - itemView.top) / 2 + icon.intrinsicHeight
                            )
                        }
                    }

                    background.draw(c)
                    icon?.draw(c)

                    super.onChildDraw(
                        c,
                        recyclerView,
                        viewHolder,
                        dX,
                        dY,
                        actionState,
                        isCurrentlyActive
                    )
                }
            })
        itemTouchHelper.attachToRecyclerView(binding.recyclerViewGalleries)

        // Load saved galleries into the RecyclerView
        val currentGalleries = customGalleries.map { name ->
            GalleryItem(name, "0 items")  // Initially set to 0 items
        }
        galleryAdapter.submitList(currentGalleries)
    }

    private fun navigateToGalleryDetail(galleryName: String) {
        // Hide the recycler view and fab
        binding.recyclerViewGalleries.visibility = View.GONE
        binding.fabAddGallery.visibility = View.GONE

        // Create a new instance of GalleryDetailFragment with the gallery name
        val fragment = GalleryDetailFragment().apply {
            arguments = Bundle().apply {
                putString("gallery_name", galleryName)
            }
        }

        // Add fragment to the activity's layout
        supportFragmentManager.beginTransaction()
            .add(android.R.id.content, fragment)
            .addToBackStack(null)
            .commit()
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

    private fun showEditGalleryDialog(galleryItem: GalleryItem) {
        // Create a custom dialog
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_edit_gallery)

        // Make dialog background transparent to show rounded corners
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // Get references to dialog views
        val nameInput = dialog.findViewById<EditText>(R.id.edit_gallery_name)
        val cancelButton = dialog.findViewById<Button>(R.id.btn_edit_gallery_cancel)
        val saveButton = dialog.findViewById<Button>(R.id.btn_edit_gallery_confirm)

        // Set initial name in the input field
        nameInput.setText(galleryItem.name)

        // Set click listeners
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        saveButton.setOnClickListener {
            val newName = nameInput.text.toString().trim()

            when {
                newName.isEmpty() -> {
                    nameInput.error = "Gallery name cannot be empty"
                }

                customGalleries.contains(newName) -> {
                    nameInput.error = "Gallery with this name already exists"
                }

                else -> {
                    // Update the gallery name in the list
                    customGalleries[customGalleries.indexOf(galleryItem.name)] = newName
                    saveGalleries()

                    // Update RecyclerView
                    val currentList = galleryAdapter.currentList.toMutableList()
                    val index = currentList.indexOfFirst { it.name == galleryItem.name }
                    if (index != -1) {
                        currentList[index] = GalleryItem(newName)
                        galleryAdapter.submitList(currentList)
                    }

                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }

    // Export all galleries and their data to a file
    private fun exportGalleries(): String {
        val gson = Gson()
        val exportedData = mutableMapOf<String, Any>()

        // Export main galleries
        exportedData["galleries"] = customGalleries

        // Export barcode data for each gallery
        val barcodeDataMap = mutableMapOf<String, List<BarcodeData>>()
        for (galleryName in customGalleries) {
            val barcodeKey = "barcodes_$galleryName"
            val barcodeJson = sharedPreferences.getString(barcodeKey, null)
            if (!barcodeJson.isNullOrEmpty()) {
                val barcodeType = object : TypeToken<List<BarcodeData>>() {}.type
                val barcodeList: List<BarcodeData> = gson.fromJson(barcodeJson, barcodeType)
                barcodeDataMap[galleryName] = barcodeList
            }
        }
        exportedData["barcodes"] = barcodeDataMap

        // Export sub-galleries for each gallery
        val subgalleriesMap = mutableMapOf<String, List<GalleryItem>>()
        for (galleryName in customGalleries) {
            val subgalleryKey = "subgalleries_$galleryName"
            val subgalleriesJson = sharedPreferences.getString(subgalleryKey, null)
            if (!subgalleriesJson.isNullOrEmpty()) {
                val subgalleriesType = object : TypeToken<List<GalleryItem>>() {}.type
                val subgalleriesList: List<GalleryItem> =
                    gson.fromJson(subgalleriesJson, subgalleriesType)
                subgalleriesMap[galleryName] = subgalleriesList
            }
        }
        exportedData["subgalleries"] = subgalleriesMap

        return gson.toJson(exportedData)
    }

    // Import galleries and their data from a file
    private fun importGalleries(jsonData: String) {
        try {
            val gson = Gson()
            val importedData = gson.fromJson(
                jsonData,
                object : TypeToken<Map<String, Any>>() {}.type
            ) as Map<String, Any>

            // Extract galleries list
            val galleriesData = importedData["galleries"] as? List<*>
            val galleries = galleriesData?.mapNotNull { it as? String } ?: return

            val editor = sharedPreferences.edit()

            // Clear existing data for galleries that will be replaced
            for (galleryName in customGalleries) {
                editor.remove("barcodes_$galleryName")
                editor.remove("subgalleries_$galleryName")
            }

            // Save new galleries
            customGalleries.clear()
            customGalleries.addAll(galleries)
            val galleriesJson = gson.toJson(customGalleries)
            editor.putString(GALLERIES_KEY, galleriesJson)

            // Import barcode data
            val barcodeData = importedData["barcodes"] as? Map<*, *>
            barcodeData?.forEach { (key, value) ->
                if (key is String && value is List<*>) {
                    try {
                        val barcodeJson = gson.toJson(value)
                        editor.putString("barcodes_$key", barcodeJson)
                    } catch (e: Exception) {
                        // Skip invalid barcode data
                    }
                }
            }

            // Import subgalleries data  
            val subgalleriesData = importedData["subgalleries"] as? Map<*, *>
            subgalleriesData?.forEach { (key, value) ->
                if (key is String && value is List<*>) {
                    try {
                        val subgalleriesJson = gson.toJson(value)
                        editor.putString("subgalleries_$key", subgalleriesJson)
                    } catch (e: Exception) {
                        // Skip invalid subgallery data
                    }
                }
            }

            editor.apply()

            // Update RecyclerView
            val currentGalleries = customGalleries.map { name ->
                GalleryItem(name, "0 items")
            }
            galleryAdapter.submitList(currentGalleries)

            Toast.makeText(
                this,
                "Import successful! ${galleries.size} galleries imported.",
                Toast.LENGTH_LONG
            ).show()

        } catch (e: Exception) {
            Toast.makeText(this, "Failed to import data: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_export -> {
                showExportDialog()
                true
            }

            R.id.action_import -> {
                showImportDialog()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    // Create and show the export menu
    private fun showExportDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_export_options)

        // Make dialog background transparent to show rounded corners
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val titleTextView = dialog.findViewById<TextView>(R.id.dialog_title)
        val messageTextView = dialog.findViewById<TextView>(R.id.dialog_message)
        val cancelButton = dialog.findViewById<Button>(R.id.btn_export_cancel)
        val fileButton = dialog.findViewById<Button>(R.id.btn_export_file)
        val emailButton = dialog.findViewById<Button>(R.id.btn_export_email)

        titleTextView.text = "Export Data"
        messageTextView.text = "Choose how to export your galleries and barcode data"

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        fileButton.setOnClickListener {
            dialog.dismiss()
            val fileName = "Honeypot_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.json"
            exportLauncher.launch(fileName)
        }

        emailButton.setOnClickListener {
            dialog.dismiss()
            exportViaEmail()
        }

        dialog.show()
    }

    // Export data via email
    private fun exportViaEmail() {
        try {
            val exportedData = exportGalleries()
            val fileName = "Honeypot_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.json"
            
            // Create a temporary file
            val tempFile = File(cacheDir, fileName)
            tempFile.writeText(exportedData)
            
            // Create content URI for the file
            val uri = androidx.core.content.FileProvider.getUriForFile(
                this,
                "com.bar.honeypot.fileprovider",
                tempFile
            )
            
            // Create email intent
            val emailIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_EMAIL, arrayOf("")) // Empty, user can fill in
                putExtra(Intent.EXTRA_SUBJECT, "Honeypot Data Export - $fileName")
                putExtra(Intent.EXTRA_TEXT, 
                    "Hello,\n\n" +
                    "Please find attached my Honeypot barcode data export.\n\n" +
                    "Export contains:\n" +
                    "• ${customGalleries.size} galleries\n" +
                    "• All barcode data and metadata\n" +
                    "• Sub-galleries\n\n" +
                    "Generated on: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n\n" +
                    "Best regards"
                )
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            // Start email chooser
            startActivity(Intent.createChooser(emailIntent, "Send Honeypot Data via Email"))
            
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to prepare email export: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // Create and show the import menu
    private fun showImportDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_export_import)

        // Make dialog background transparent to show rounded corners
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val titleTextView = dialog.findViewById<TextView>(R.id.dialog_title)
        val messageTextView = dialog.findViewById<TextView>(R.id.dialog_message)
        val cancelButton = dialog.findViewById<Button>(R.id.btn_export_import_cancel)
        val actionButton = dialog.findViewById<Button>(R.id.btn_export_import_action)

        titleTextView.text = "Import Data"
        messageTextView.text =
            "Import galleries and barcodes from a JSON file.\n\n⚠️ This will replace all existing data!"
        actionButton.text = "Import"

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        actionButton.setOnClickListener {
            importLauncher.launch(arrayOf("application/json", "*/*"))
            dialog.dismiss()
        }

        dialog.show()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // If there are fragments in the back stack, pop them
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
        } else {
            // Otherwise, use the default behavior
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }
}
