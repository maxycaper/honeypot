package com.bar.honeypot

import android.content.Context
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
import android.content.SharedPreferences
import android.widget.Button
import androidx.recyclerview.widget.LinearLayoutManager
import com.bar.honeypot.ui.gallery.GalleryListAdapter
import com.bar.honeypot.ui.gallery.GalleryDetailFragment
import com.bar.honeypot.ui.gallery.GalleryItem
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import java.util.ArrayList
import androidx.core.view.WindowCompat

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val customGalleries = ArrayList<String>()
    private lateinit var sharedPreferences: SharedPreferences
    private val PREFS_NAME = "HoneypotPrefs"
    private val GALLERIES_KEY = "savedGalleries"
    private lateinit var galleryAdapter: GalleryListAdapter

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
