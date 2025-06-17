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
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
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
import androidx.core.view.GravityCompat
import android.widget.Button
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Window
import android.app.Dialog

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private val customGalleries = mutableListOf<String>()
    private lateinit var sharedPreferences: SharedPreferences
    private val PREFS_NAME = "HoneypotPrefs"
    private val GALLERIES_KEY = "savedGalleries"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)
        
        // Force navigation icon to be white
        val navigationIcon = androidx.appcompat.content.res.AppCompatResources.getDrawable(this, R.drawable.ic_neon_menu)
        navigationIcon?.setTint(getColor(R.color.white))
        binding.appBarMain.toolbar.navigationIcon = navigationIcon

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        // Load saved galleries
        loadSavedGalleries()

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        
        // Set app version in the navigation header
        setAppVersion()
        
        // Set up gallery management controls
        setupGalleryManagement(navView, navController)
        
        // Configure toolbar to open drawer when menu icon is clicked
        binding.appBarMain.toolbar.setNavigationOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }
        
        // Configure app bar with empty top-level destinations
        appBarConfiguration = AppBarConfiguration(emptySet(), drawerLayout)
        setupActionBarWithNavController(navController, appBarConfiguration)
        
        // Restore saved galleries to the menu
        restoreGalleriesToMenu(navView, navController)
        
        // If there are no galleries, show the create gallery dialog
        if (customGalleries.isEmpty()) {
            showAddGalleryDialog(navView, navController)
        } else {
            // Navigate to the first gallery
            val firstGallery = customGalleries.first()
            val bundle = bundleOf("gallery_name" to firstGallery)
            navController.navigate(R.id.nav_gallery, bundle)
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
    
    private fun restoreGalleriesToMenu(navView: NavigationView, navController: NavController) {
        val menu = navView.menu
        
        // Add each saved gallery to the menu
        customGalleries.forEach { galleryName ->
            val newItemId = View.generateViewId()
            val newItem = menu.add(R.id.gallery_group, newItemId, Menu.NONE, galleryName)
            newItem.setIcon(R.drawable.ic_menu_gallery)
            newItem.isCheckable = true
            
            // Add delete icon to each gallery
            newItem.setActionView(R.layout.menu_item_delete)
            val deleteView = newItem.actionView
            deleteView?.findViewById<View>(R.id.btn_delete_gallery)?.setOnClickListener {
                showDeleteGalleryDialog(galleryName, navView, navController)
            }
        }
        
        // Set navigation listener for all menu items
        navView.setNavigationItemSelectedListener { menuItem ->
            val menuTitle = menuItem.title.toString()
            
            // Check if this is a custom gallery
            if (customGalleries.contains(menuTitle)) {
                // Navigate to gallery fragment with gallery name as argument
                val bundle = bundleOf("gallery_name" to menuTitle)
                navController.navigate(R.id.nav_gallery, bundle)
                binding.drawerLayout.closeDrawers()
                return@setNavigationItemSelectedListener true
            }
            
            // Let the default listener handle other menu items
            menuItem.isChecked = true
            navController.navigate(menuItem.itemId)
            binding.drawerLayout.closeDrawers()
            true
        }
    }
    
    private fun showDeleteGalleryDialog(galleryName: String, navView: NavigationView, navController: NavController) {
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
        messageTextView.text = "Are you sure you want to delete the gallery '$galleryName'? This action cannot be undone."
        
        // Set click listeners
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }
        
        deleteButton.setOnClickListener {
            deleteGallery(galleryName, navView, navController)
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    private fun deleteGallery(galleryName: String, navView: NavigationView, navController: NavController) {
        // Remove gallery from list
        customGalleries.remove(galleryName)
        
        // Save updated galleries list
        saveGalleries()
        
        // Delete associated barcodes
        deleteGalleryBarcodes(galleryName)
        
        // Rebuild the navigation menu
        rebuildNavigationMenu(navView, navController)
        
        // If all galleries are deleted, show the create gallery dialog
        if (customGalleries.isEmpty()) {
            showAddGalleryDialog(navView, navController)
        } else {
            // Navigate to another gallery if we're in the deleted gallery
            if (navController.currentBackStackEntry?.arguments?.getString("gallery_name") == galleryName) {
                val firstGallery = customGalleries.first()
                val bundle = bundleOf("gallery_name" to firstGallery)
                navController.navigate(R.id.nav_gallery, bundle)
            }
        }
        
        Toast.makeText(this, "Gallery '$galleryName' deleted", Toast.LENGTH_SHORT).show()
    }
    
    private fun deleteGalleryBarcodes(galleryName: String) {
        // Delete the associated barcodes from SharedPreferences
        val editor = sharedPreferences.edit()
        editor.remove("barcodes_$galleryName")
        editor.apply()
    }
    
    private fun rebuildNavigationMenu(navView: NavigationView, navController: NavController) {
        // Clear the gallery group
        val menu = navView.menu
        menu.removeGroup(R.id.gallery_group)
        
        // Re-add the group
        menu.addSubMenu(Menu.NONE, R.id.gallery_group, Menu.NONE, "")
        
        // Restore galleries to menu
        restoreGalleriesToMenu(navView, navController)
    }
    
    private fun showAddGalleryDialog(navView: NavigationView, navController: NavController) {
        // Create a custom dialog
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_create_gallery)
        
        // Make dialog background transparent to show rounded corners
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        
        // Get references to dialog views
        val input = dialog.findViewById<EditText>(R.id.edit_gallery_name)
        val cancelButton = dialog.findViewById<Button>(R.id.btn_create_gallery_cancel)
        val createButton = dialog.findViewById<Button>(R.id.btn_create_gallery_confirm)
        
        // Set click listeners
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }
        
        createButton.setOnClickListener {
            val galleryName = input.text.toString().trim()
            if (galleryName.isNotEmpty()) {
                createNewGallery(galleryName, navView, navController)
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Gallery name cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }
        
        dialog.show()
    }
    
    private fun createNewGallery(galleryName: String, navView: NavigationView, navController: NavController) {
        if (!customGalleries.contains(galleryName)) {
            // Add new gallery to the list
            customGalleries.add(galleryName)
            
            // Save galleries to SharedPreferences
            saveGalleries()
            
            // Add new gallery to the navigation menu
            val menu = navView.menu
            val newItemId = View.generateViewId()
            val newItem = menu.add(R.id.gallery_group, newItemId, Menu.NONE, galleryName)
            newItem.setIcon(R.drawable.ic_menu_gallery)
            newItem.isCheckable = true
            // Add delete icon to the new gallery (same as restoreGalleriesToMenu)
            newItem.setActionView(R.layout.menu_item_delete)
            val deleteView = newItem.actionView
            deleteView?.findViewById<View>(R.id.btn_delete_gallery)?.setOnClickListener {
                showDeleteGalleryDialog(galleryName, navView, navController)
            }
            
            Toast.makeText(this, "Gallery '$galleryName' created", Toast.LENGTH_SHORT).show()
            
            // Navigate to the new gallery immediately
            val bundle = bundleOf("gallery_name" to galleryName)
            navController.navigate(R.id.nav_gallery, bundle)
            binding.drawerLayout.closeDrawers()
        } else {
            Toast.makeText(this, "Gallery '$galleryName' already exists", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun setupGalleryManagement(navView: NavigationView, navController: NavController) {
        val headerView = navView.getHeaderView(0)
        
        // Set up the FAB in the header
        val fabAddGalleryHeader = headerView.findViewById<FloatingActionButton>(R.id.fab_add_gallery_header)
        fabAddGalleryHeader.setOnClickListener {
            showAddGalleryDialog(navView, navController)
        }
    }

    private fun setAppVersion() {
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val versionName = packageInfo.versionName ?: "Unknown"
            val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
            
            // Format the version as per our scheme (YY.MM.VV)
            val (year, month, version) = try {
                VersionHelper.parseVersionName(versionName)
            } catch (e: Exception) {
                Triple(0, 0, 0) // Default values if parsing fails
            }
            
            val formattedVersion = when {
                year > 0 -> "v${versionName} (${year}/Q${(month-1)/3 + 1}/P${version})"
                else -> "v${versionName}"
            }
            
            val navHeaderView = binding.navView.getHeaderView(0)
            val versionTextView = navHeaderView.findViewById<TextView>(R.id.version_text)
            versionTextView.text = formattedVersion
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}