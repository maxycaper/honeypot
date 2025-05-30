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

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        // Load saved galleries
        loadSavedGalleries()

        setSupportActionBar(binding.appBarMain.toolbar)

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        
        // Set app version in the navigation header
        setAppVersion()
        
        // Set up gallery management controls
        setupGalleryManagement(navView, navController)
        
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_gallery
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        
        // Restore saved galleries to the menu
        restoreGalleriesToMenu(navView, navController)
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
    
    private fun showAddGalleryDialog(navView: NavigationView, navController: NavController) {
        // Create an EditText for the dialog
        val input = EditText(this)
        input.hint = "Enter gallery name"
        
        // Configure the input layout
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(50, 20, 50, 20)
        input.layoutParams = layoutParams
        
        // Create and show the dialog
        AlertDialog.Builder(this)
            .setTitle("Create New Gallery")
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                val galleryName = input.text.toString().trim()
                if (galleryName.isNotEmpty()) {
                    createNewGallery(galleryName, navView, navController)
                } else {
                    Toast.makeText(this, "Gallery name cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
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