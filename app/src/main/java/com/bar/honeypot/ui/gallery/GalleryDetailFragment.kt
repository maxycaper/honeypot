package com.bar.honeypot.ui.gallery

import android.app.Dialog
import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.Window
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ItemTouchHelper
import com.bar.honeypot.R
import com.bar.honeypot.model.BarcodeData
import com.bar.honeypot.ui.gallery.GalleryListAdapter
import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Environment
import android.util.DisplayMetrics
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.AdapterView
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GalleryDetailFragment : Fragment() {

    private var galleryName: String = "Gallery"
    private lateinit var subgalleriesAdapter: GalleryListAdapter
    private lateinit var barcodeAdapter: BarcodeAdapter
    private var isSubgallery: Boolean = false // Flag to indicate if this is a sub-gallery

    // View references
    private lateinit var galleryNameText: TextView
    private lateinit var subgalleriesLabel: TextView
    private lateinit var barcodesLabel: TextView
    private lateinit var recyclerViewSubgalleries: RecyclerView
    private lateinit var recyclerViewBarcodes: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var fabAdd: FloatingActionButton

    private val subgalleries = mutableListOf<GalleryItem>()
    private val barcodes = mutableListOf<BarcodeData>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_gallery_detail, container, false)

        // Handle status bar and navigation bar insets
        view.setOnApplyWindowInsetsListener { v, insets ->
            val statusBarHeight = insets.systemWindowInsetTop
            val navigationBarHeight = insets.systemWindowInsetBottom
            
            // Add top margin to header background and back button to account for status bar
            val headerBackground = view.findViewById<View>(R.id.headerBackground)
            val headerParams = headerBackground.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            headerParams.topMargin = statusBarHeight
            headerBackground.layoutParams = headerParams

            // Add bottom padding to RecyclerView to avoid overlap with navigation bar
            val recyclerViewBarcodes = view.findViewById<RecyclerView>(R.id.recyclerViewBarcodes)
            recyclerViewBarcodes.setPadding(
                recyclerViewBarcodes.paddingLeft,
                recyclerViewBarcodes.paddingTop,
                recyclerViewBarcodes.paddingRight,
                80 + navigationBarHeight // FAB space + navigation bar height
            )

            // Add bottom margin to FAB to avoid overlap with navigation bar
            val fabAdd = view.findViewById<FloatingActionButton>(R.id.fabAdd)
            val fabParams = fabAdd.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            fabParams.bottomMargin = 16 + navigationBarHeight // Original margin + navigation bar height
            fabAdd.layoutParams = fabParams
            
            insets
        }

        // Get gallery name from arguments
        arguments?.let {
            galleryName = it.getString("gallery_name", "Gallery")
            isSubgallery = it.getBoolean("is_subgallery", false)
        }

        // Initialize views
        initializeViews(view)

        // Set up UI
        setupUI()

        // Load data
        loadGalleryContent()

        return view
    }

    private fun initializeViews(view: View) {
        galleryNameText = view.findViewById(R.id.galleryNameText)
        subgalleriesLabel = view.findViewById(R.id.subgalleriesLabel)
        barcodesLabel = view.findViewById(R.id.barcodesLabel)
        recyclerViewSubgalleries = view.findViewById(R.id.recyclerViewSubgalleries)
        recyclerViewBarcodes = view.findViewById(R.id.recyclerViewBarcodes)
        emptyView = view.findViewById(R.id.emptyView)
        fabAdd = view.findViewById(R.id.fabAdd)

        val backButton: ImageButton = view.findViewById(R.id.backButton)
        backButton.setOnClickListener {
            // Go back to previous screen
            activity?.supportFragmentManager?.popBackStack()
        }
    }

    private fun setupUI() {
        // Set gallery name in the header
        galleryNameText.text = galleryName

        // Hide action bar since we have our own header
        (activity as? AppCompatActivity)?.supportActionBar?.hide()

        // Setup subgalleries RecyclerView
        recyclerViewSubgalleries.layoutManager = LinearLayoutManager(context)
        subgalleriesAdapter = GalleryListAdapter(
            onItemClick = { galleryItem ->
                // Navigate to sub-gallery when clicked
                navigateToSubgallery(galleryItem.name)
            },
            onArrowClick = { galleryItem ->
                // Navigate to nested gallery
                navigateToSubgallery(galleryItem.name)
            }
        )
        recyclerViewSubgalleries.adapter = subgalleriesAdapter

        // Add swipe functionality for sub-galleries
        val subgalleryItemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
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
                if (position != RecyclerView.NO_POSITION && position < subgalleries.size) {
                    val subgallery = subgalleries[position]

                    // Restore the item to its original state (cancel the swipe)
                    subgalleriesAdapter.notifyItemChanged(position)

                    // Handle based on direction
                    if (direction == ItemTouchHelper.RIGHT) {
                        // Swipe right - Edit/Update subgallery
                        showEditSubgalleryDialog(subgallery, position)
                    } else {
                        // Swipe left - Delete subgallery
                        showDeleteSubgalleryDialog(subgallery, position)
                    }
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

                if (dX > 0) { // Swiping to the right (edit)
                    background.color =
                        ContextCompat.getColor(requireContext(), R.color.neon_hot_pink)
                    background.setBounds(
                        itemView.left,
                        itemView.top,
                        itemView.left + dX.toInt(),
                        itemView.bottom
                    )
                } else { // Swiping to the left (delete)
                    background.color =
                        ContextCompat.getColor(requireContext(), android.R.color.holo_red_light)
                    background.setBounds(
                        itemView.right + dX.toInt(),
                        itemView.top,
                        itemView.right,
                        itemView.bottom
                    )
                }

                background.draw(c)

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

        subgalleryItemTouchHelper.attachToRecyclerView(recyclerViewSubgalleries)

        // Setup barcodes RecyclerView
        setupBarcodesRecyclerView()

        // Setup FAB
        fabAdd.setOnClickListener {
            showAddOptionsDialog()
        }

        // Make sure the icon is white and stands out
        fabAdd.setColorFilter(Color.WHITE)

        // Add a slight elevation for better shadow
        fabAdd.elevation = 8f
    }

    private fun setupBarcodesRecyclerView() {
        barcodeAdapter = BarcodeAdapter(
            context = requireContext(),
            onItemClick = { barcode, position ->
                // Show barcode details
                showBarcodeDetails(barcode, position)
            }
        )

        recyclerViewBarcodes.apply {
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
                if (position != RecyclerView.NO_POSITION && position < barcodes.size) {
                    val deletedBarcode = barcodes[position]

                    // Restore the item to its original state (cancel the swipe)
                    barcodeAdapter.notifyItemChanged(position)

                    // Show delete confirmation dialog
                    showDeleteBarcodeDialog(deletedBarcode, position)
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
                background.color =
                    ContextCompat.getColor(requireContext(), android.R.color.holo_red_light)

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

        itemTouchHelper.attachToRecyclerView(recyclerViewBarcodes)
    }

    private fun showDeleteBarcodeDialog(barcode: BarcodeData, position: Int) {
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
                barcodes.removeAt(position)
                barcodeAdapter.updateBarcodes(barcodes)
                saveBarcodesToSharedPreferences()

                // Update visibility of barcodes section
                updateContentVisibility()

                // Show confirmation
                Toast.makeText(
                    context,
                    "Barcode deleted from $galleryName",
                    Toast.LENGTH_SHORT
                ).show()

                dialog.dismiss()
            }

            dialog.show()
        }
    }

    private fun loadGalleryContent() {
        // Load subgalleries for this gallery
        loadSubgalleries()

        // Load barcodes for this gallery
        loadBarcodes()

        // Update UI based on content
        updateContentVisibility()
    }

    private fun loadSubgalleries() {
        // If this is a sub-gallery, we shouldn't show or load nested sub-galleries
        if (isSubgallery) {
            subgalleriesLabel.visibility = View.GONE
            recyclerViewSubgalleries.visibility = View.GONE
            return
        }

        // Get shared preferences
        val sharedPreferences = requireActivity().getSharedPreferences("HoneypotPrefs", Context.MODE_PRIVATE)

        // Get subgalleries for current gallery
        val subgalleriesJson = sharedPreferences.getString("subgalleries_$galleryName", null)
        if (!subgalleriesJson.isNullOrEmpty()) {
            try {
                val type = object : TypeToken<List<GalleryItem>>() {}.type
                val loadedSubgalleries: List<GalleryItem> = Gson().fromJson(subgalleriesJson, type)
                subgalleries.clear()
                subgalleries.addAll(loadedSubgalleries)
                Handler(Looper.getMainLooper()).postDelayed({
                    subgalleriesAdapter.submitList(subgalleries)
                }, 50)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadBarcodes() {
        // Get shared preferences
        val sharedPreferences = requireActivity().getSharedPreferences("HoneypotPrefs", Context.MODE_PRIVATE)

        // Get barcodes for this gallery
        val barcodesJson = sharedPreferences.getString("barcodes_$galleryName", null)
        if (!barcodesJson.isNullOrEmpty()) {
            try {
                val type = object : TypeToken<List<BarcodeData>>() {}.type
                val loadedBarcodes: List<BarcodeData> = Gson().fromJson(barcodesJson, type)
                barcodes.clear()
                barcodes.addAll(loadedBarcodes)
                barcodeAdapter.updateBarcodes(barcodes)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updateContentVisibility() {
        // Show/hide subgalleries section (always hide for sub-galleries)
        if (isSubgallery || subgalleries.isEmpty()) {
            subgalleriesLabel.visibility = View.GONE
            recyclerViewSubgalleries.visibility = View.GONE
        } else {
            subgalleriesLabel.visibility = View.VISIBLE
            recyclerViewSubgalleries.visibility = View.VISIBLE
        }

        // Show/hide barcodes section
        if (barcodes.isEmpty()) {
            barcodesLabel.visibility = View.GONE
            recyclerViewBarcodes.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
        } else {
            barcodesLabel.visibility = View.VISIBLE
            recyclerViewBarcodes.visibility = View.VISIBLE
            emptyView.visibility = View.GONE
        }

        // If both are empty, show empty view
        if ((isSubgallery || subgalleries.isEmpty()) && barcodes.isEmpty()) {
            emptyView.visibility = View.VISIBLE
        }
    }

    private fun navigateToSubgallery(subgalleryName: String) {
        // Create a new instance of GalleryDetailFragment with the subgallery name
        val fragment = GalleryDetailFragment().apply {
            arguments = Bundle().apply {
                putString("gallery_name", subgalleryName)
                putBoolean("is_subgallery", true) // Indicate this is a sub-gallery
            }
        }

        // Add the new fragment on top of the current one
        parentFragmentManager.beginTransaction()
            .add(android.R.id.content, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun showBarcodeDetails(barcode: BarcodeData, position: Int) {
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
            val addPhotoButton = dialog.findViewById<Button>(R.id.btn_add_product_photo)
            val takeNewPhotoButton = dialog.findViewById<Button>(R.id.btn_take_new_photo)
            val removePhotoButton = dialog.findViewById<Button>(R.id.btn_remove_photo)
            val descriptionView = dialog.findViewById<TextView>(R.id.barcode_description)
            val closeButton = dialog.findViewById<Button>(R.id.btn_display_close)
            val editTitleButton = dialog.findViewById<Button>(R.id.btn_display_edit_title)

            // Set informative title based on content - prioritize custom title over product name
            val title = when {
                barcode.title.isNotEmpty() -> barcode.title
                barcode.productName.isNotEmpty() && !barcode.productName.startsWith("Product:") -> barcode.productName
                barcode.url.isNotEmpty() -> "URL Barcode"
                barcode.email.isNotEmpty() -> "Email Barcode"
                barcode.phone.isNotEmpty() -> "Phone Barcode"
                barcode.wifiSsid.isNotEmpty() -> "WiFi Network"
                barcode.contactInfo.isNotEmpty() -> "Contact Information"
                barcode.geoLat != 0.0 && barcode.geoLng != 0.0 -> "Location"
                else -> "Barcode Details"
            }
            titleTextView.text = title

            // Always show edit title button
            editTitleButton.visibility = View.VISIBLE

            valueTextView.text = barcode.value

            // Enhanced format display
            val formatText = StringBuilder("Format: ${barcode.format}")

            // Add metadata type if barcode has additional data
            if (barcode.hasMetadata()) {
                formatText.append(" (${barcode.getMetadataType()})")
            }

            formatTextView.text = formatText.toString()

            // Enhanced product image display and photo management
            val hasProductImage = barcode.productImageUrl.isNotEmpty()
            val hasLocalImage = barcode.productImageUrl.startsWith("/") // Local file path

            if (hasProductImage) {
                if (hasLocalImage) {
                    // Load local captured image with orientation correction
                    try {
                        var bitmap = BitmapFactory.decodeFile(barcode.productImageUrl)
                        if (bitmap != null) {
                            // Read Exif orientation and rotate if needed
                            val exif = android.media.ExifInterface(barcode.productImageUrl)
                            val orientation = exif.getAttributeInt(
                                android.media.ExifInterface.TAG_ORIENTATION,
                                android.media.ExifInterface.ORIENTATION_NORMAL
                            )
                            val matrix = android.graphics.Matrix()
                            when (orientation) {
                                android.media.ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(
                                    90f
                                )

                                android.media.ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(
                                    180f
                                )

                                android.media.ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(
                                    270f
                                )

                                android.media.ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(
                                    -1f,
                                    1f
                                )

                                android.media.ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(
                                    1f,
                                    -1f
                                )
                            }
                            if (orientation != android.media.ExifInterface.ORIENTATION_NORMAL) {
                                bitmap = Bitmap.createBitmap(
                                    bitmap,
                                    0,
                                    0,
                                    bitmap.width,
                                    bitmap.height,
                                    matrix,
                                    true
                                )
                            }
                            productImageView.setImageBitmap(bitmap)
                            productImageView.visibility = View.VISIBLE
                            addPhotoButton.visibility = View.GONE
                            removePhotoButton.visibility = View.VISIBLE
                        } else {
                            // Image file not found, show initial state
                            productImageView.visibility = View.GONE
                            addPhotoButton.visibility = View.VISIBLE
                            removePhotoButton.visibility = View.GONE
                        }
                    } catch (e: Exception) {
                        productImageView.visibility = View.GONE
                        addPhotoButton.visibility = View.VISIBLE
                        removePhotoButton.visibility = View.GONE
                    }
                } else {
                    // URL-based image
                    productImageView.visibility = View.VISIBLE
                    addPhotoButton.visibility = View.GONE
                    removePhotoButton.visibility = View.VISIBLE

                    // Load remote image with improved URL detection
                    if (barcode.productImageUrl.startsWith("http")) {
                        // Load the image in a background thread
                        Thread {
                            try {
                                val url = java.net.URL(barcode.productImageUrl)
                                val connection = url.openConnection() as java.net.HttpURLConnection
                                connection.doInput = true
                                connection.connectTimeout = 10000 // 10 second timeout
                                connection.readTimeout = 10000
                                connection.connect()
                                val input = connection.inputStream
                                val bitmap = BitmapFactory.decodeStream(input)
                                input.close()
                                connection.disconnect()

                                // Update UI on main thread
                                activity?.runOnUiThread {
                                    if (bitmap != null) {
                                        productImageView.setImageBitmap(bitmap)
                                    } else {
                                        // Failed to decode image, show camera button instead
                                        productImageView.visibility = View.GONE
                                        addPhotoButton.visibility = View.VISIBLE
                                        removePhotoButton.visibility = View.GONE
                                    }
                                }
                            } catch (e: Exception) {
                                // Error loading image, show camera button instead
                                activity?.runOnUiThread {
                                    productImageView.visibility = View.GONE
                                    addPhotoButton.visibility = View.VISIBLE
                                    removePhotoButton.visibility = View.GONE
                                }
                            }
                        }.start()
                    } else {
                        // Invalid URL, show camera button instead
                        productImageView.visibility = View.GONE
                        addPhotoButton.visibility = View.VISIBLE
                        removePhotoButton.visibility = View.GONE
                    }
                }
            } else {
                // No product image available, show initial camera button
                productImageView.visibility = View.GONE
                addPhotoButton.visibility = View.VISIBLE
                removePhotoButton.visibility = View.GONE
            }

            // Set up photo management button listeners
            addPhotoButton.setOnClickListener {
                dialog.dismiss()
                dispatchTakePictureIntent(barcode)
            }

            takeNewPhotoButton.setOnClickListener {
                dialog.dismiss()
                dispatchTakePictureIntent(barcode)
            }

            removePhotoButton.setOnClickListener {
                // Remove the image reference from the barcode
                if (hasLocalImage && barcode.productImageUrl.isNotEmpty()) {
                    try {
                        val file = File(barcode.productImageUrl)
                        if (file.exists()) {
                            file.delete()
                        }
                    } catch (e: Exception) {
                        // Ignore
                    }
                }

                // Update barcode and save
                val updatedBarcode = barcode.copy(productImageUrl = "")
                val index = barcodes.indexOf(barcode)
                if (index != -1) {
                    barcodes[index] = updatedBarcode
                    barcodeAdapter.updateBarcodes(barcodes)
                    saveBarcodesToSharedPreferences()
                    Toast.makeText(context, "Product photo removed", Toast.LENGTH_SHORT).show()

                    // Refresh dialog
                    dialog.dismiss()
                    showBarcodeDetails(updatedBarcode, position)
                }
            }

            // Generate and display the barcode
            generateBarcodeImage(barcode.value, barcode.format, barcodeImageView)

            // Add double-tap functionality to the barcode image for enlargement
            var lastClickTime = 0L
            val doubleTapThreshold = 300L
            barcodeImageView.setOnClickListener {
                val currentTime = System.currentTimeMillis()
                val timeDiff = currentTime - lastClickTime

                if (timeDiff < doubleTapThreshold && lastClickTime > 0) {
                    dialog.dismiss() // Close current dialog first
                    showEnlargedBarcodeDialog(barcode) // Show enlarged version
                    lastClickTime = 0 // Reset to prevent triple-tap
                } else {
                    lastClickTime = currentTime
                }
            }

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
                    metadataText.append("<b>Barcode:</b> ${barcode.productName.substringAfter("Product: ")}")
                        .append("<br><br>")
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
                metadataText.append(
                    "<b>Contact Info:</b><br>${
                        barcode.contactInfo.replace(
                            "\n",
                            "<br>"
                        )
                    }"
                ).append("<br><br>")
            }

            if (barcode.geoLat != 0.0 && barcode.geoLng != 0.0) {
                metadataText.append("<b>Location:</b> ${barcode.geoLat}, ${barcode.geoLng}")
                    .append("<br><br>")
            }

            // Set description text or hide if none
            if (metadataText.isNotEmpty()) {
                descriptionView.text = android.text.Html.fromHtml(
                    metadataText.toString().trim(),
                    android.text.Html.FROM_HTML_MODE_COMPACT
                )
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
                showEditBarcodeDialog(barcode, position)
            }

            dialog.show()
        }
    }

    private fun showEditBarcodeDialog(barcode: BarcodeData, position: Int) {
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
                    val updatedBarcode = barcode.copy(title = newTitle)
                    val index = barcodes.indexOf(barcode)
                    if (index != -1) {
                        barcodes[index] = updatedBarcode
                        barcodeAdapter.updateBarcodes(barcodes)
                        saveBarcodesToSharedPreferences()
                        Toast.makeText(ctx, "Title updated", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(ctx, "Title cannot be empty", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                dialog.dismiss()

                // Show the display dialog again with updated information
                showBarcodeDetails(barcodes[position], position)
            }

            dialog.show()
        }
    }

    private fun generateBarcodeImage(
        barcodeValue: String,
        barcodeFormat: String,
        imageView: ImageView
    ) {
        try {
            // Determine the barcode format to use
            val format = when (barcodeFormat) {
                "QR_CODE" -> BarcodeFormat.QR_CODE
                "CODE_128" -> BarcodeFormat.CODE_128
                "CODE_39" -> BarcodeFormat.CODE_39
                "CODE_93" -> BarcodeFormat.CODE_93
                "EAN_13" -> BarcodeFormat.EAN_13
                "EAN_8" -> BarcodeFormat.EAN_8
                "UPC_A" -> BarcodeFormat.UPC_A
                "UPC_E" -> BarcodeFormat.UPC_E
                "DATA_MATRIX" -> BarcodeFormat.DATA_MATRIX
                "AZTEC" -> BarcodeFormat.AZTEC
                "PDF417" -> BarcodeFormat.PDF_417
                "CODABAR" -> BarcodeFormat.CODABAR
                "ITF" -> BarcodeFormat.ITF
                else -> BarcodeFormat.QR_CODE // Default to QR code
            }

            // Create BarcodeEncoder to generate the barcode
            val writer = BarcodeEncoder()

            // Adjust width for CODE_128
            val width = if (barcodeFormat == "CODE_128" && barcodeValue.length > 20) 800 else 512
            val height = if (barcodeFormat == "CODE_128" && barcodeValue.length > 20) 200 else 512

            val bitmap = writer.encodeBitmap(barcodeValue, format, width, height)

            // Set the bitmap to the ImageView
            imageView.setImageBitmap(bitmap)
        } catch (e: Exception) {
            // If there's an error generating the barcode, show an error message
            Toast.makeText(context, "Error generating barcode: ${e.message}", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun showBarcodeActionChoiceDialog() {
        context?.let { ctx ->
            // Create a custom dialog
            val dialog = Dialog(ctx)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(R.layout.dialog_barcode_action_choice)

            // Make dialog background transparent to show rounded corners
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            // Update dialog title
            val titleTextView = dialog.findViewById<TextView>(R.id.dialog_title)
            titleTextView.text = "Add Barcode to $galleryName"

            // Set up dialog views
            val scanOption = dialog.findViewById<View>(R.id.option_scan)
            val manualOption = dialog.findViewById<View>(R.id.option_manual)
            val cancelButton = dialog.findViewById<Button>(R.id.btn_cancel)

            // Set click listeners
            scanOption.setOnClickListener {
                dialog.dismiss()
                checkCameraPermissionAndScan()
            }

            manualOption.setOnClickListener {
                dialog.dismiss()
                showManualBarcodeEntryDialog()
            }

            cancelButton.setOnClickListener {
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

                // Add the barcode to the gallery's barcodes
                addBarcode(barcodeValue, barcodeFormat, barcodeTitle)
                dialog.dismiss()
            }

            dialog.show()
        }
    }

    private fun addBarcode(value: String, format: String, title: String = "") {
        // Check if this barcode already exists in the gallery
        if (barcodes.any { it.value == value }) {
            // Show alert for duplicate barcode instead of adding
            context?.let { ctx ->
                val dialog = Dialog(ctx)
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
                dialog.setContentView(R.layout.dialog_duplicate_barcode)
                dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

                // Set up dialog views
                val messageTextView = dialog.findViewById<TextView>(R.id.dialog_message)
                val barcodeValueTextView = dialog.findViewById<TextView>(R.id.barcode_value)
                val okButton = dialog.findViewById<Button>(R.id.btn_ok)

                // Set the message and barcode value
                messageTextView.text = "This barcode already exists in '$galleryName'."
                barcodeValueTextView.text = value

                // Set button listener
                okButton.setOnClickListener {
                    dialog.dismiss()
                }

                dialog.show()
            }
            return
        }

        // Create new barcode
        val newBarcode = BarcodeData(
            value = value,
            format = format,
            galleryName = galleryName,
            title = title
        )

        // Add to the list
        barcodes.add(newBarcode)

        // Update the adapter
        barcodeAdapter.updateBarcodes(barcodes)

        // Save to SharedPreferences
        saveBarcodesToSharedPreferences()

        // Update visibility of barcodes section
        updateContentVisibility()

        // Show confirmation
        Toast.makeText(
            context,
            "Barcode added to $galleryName",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun saveBarcodesToSharedPreferences() {
        val sharedPreferences = requireActivity().getSharedPreferences("HoneypotPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // Convert list to JSON
        val barcodesJson = Gson().toJson(barcodes)

        // Save to SharedPreferences with the current gallery as the parent
        editor.putString("barcodes_$galleryName", barcodesJson)
        editor.apply()
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
            val barcodeValue =
                data?.getStringExtra(com.bar.honeypot.ui.scanner.BarcodeScannerActivity.BARCODE_VALUE)
            val barcodeFormat =
                data?.getStringExtra(com.bar.honeypot.ui.scanner.BarcodeScannerActivity.BARCODE_FORMAT)

            // Extract all product information from scanner result
            val productName =
                data?.getStringExtra(com.bar.honeypot.ui.scanner.BarcodeScannerActivity.BARCODE_PRODUCT_NAME)
                    ?: ""
            val productTitle =
                data?.getStringExtra(com.bar.honeypot.ui.scanner.BarcodeScannerActivity.BARCODE_TITLE)
                    ?: ""
            val productBrand =
                data?.getStringExtra(com.bar.honeypot.ui.scanner.BarcodeScannerActivity.BARCODE_PRODUCT_BRAND)
                    ?: ""
            val productImageUrl =
                data?.getStringExtra(com.bar.honeypot.ui.scanner.BarcodeScannerActivity.BARCODE_PRODUCT_IMAGE_URL)
                    ?: ""

            // Extract all other structured data
            val description =
                data?.getStringExtra(com.bar.honeypot.ui.scanner.BarcodeScannerActivity.BARCODE_DESCRIPTION)
                    ?: ""
            val url =
                data?.getStringExtra(com.bar.honeypot.ui.scanner.BarcodeScannerActivity.BARCODE_URL)
                    ?: ""
            val email =
                data?.getStringExtra(com.bar.honeypot.ui.scanner.BarcodeScannerActivity.BARCODE_EMAIL)
                    ?: ""
            val phone =
                data?.getStringExtra(com.bar.honeypot.ui.scanner.BarcodeScannerActivity.BARCODE_PHONE)
                    ?: ""
            val smsContent =
                data?.getStringExtra(com.bar.honeypot.ui.scanner.BarcodeScannerActivity.BARCODE_SMS)
                    ?: ""
            val wifiSsid =
                data?.getStringExtra(com.bar.honeypot.ui.scanner.BarcodeScannerActivity.BARCODE_WIFI_SSID)
                    ?: ""
            val wifiPassword =
                data?.getStringExtra(com.bar.honeypot.ui.scanner.BarcodeScannerActivity.BARCODE_WIFI_PASSWORD)
                    ?: ""
            val wifiType =
                data?.getStringExtra(com.bar.honeypot.ui.scanner.BarcodeScannerActivity.BARCODE_WIFI_TYPE)
                    ?: ""
            val geoLat = data?.getDoubleExtra(
                com.bar.honeypot.ui.scanner.BarcodeScannerActivity.BARCODE_GEO_LAT,
                0.0
            ) ?: 0.0
            val geoLng = data?.getDoubleExtra(
                com.bar.honeypot.ui.scanner.BarcodeScannerActivity.BARCODE_GEO_LNG,
                0.0
            ) ?: 0.0
            val contactInfo =
                data?.getStringExtra(com.bar.honeypot.ui.scanner.BarcodeScannerActivity.BARCODE_CONTACT_INFO)
                    ?: ""

            if (barcodeValue != null && barcodeFormat != null) {
                // Determine the best title to use
                val titleToUse = when {
                    productName.isNotEmpty() -> productName
                    productTitle.isNotEmpty() -> productTitle
                    else -> ""
                }

                // Add the scanned barcode with all information
                addBarcodeWithFullInfo(
                    value = barcodeValue,
                    format = barcodeFormat,
                    title = titleToUse,
                    description = if (productBrand.isNotEmpty()) "Brand: $productBrand" else description,
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
            } else {
                Toast.makeText(context, "Error: Invalid barcode data", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun addBarcodeWithFullInfo(
        value: String,
        format: String,
        title: String = "",
        description: String = "",
        url: String = "",
        email: String = "",
        phone: String = "",
        smsContent: String = "",
        wifiSsid: String = "",
        wifiPassword: String = "",
        wifiType: String = "",
        geoLat: Double = 0.0,
        geoLng: Double = 0.0,
        productName: String = "",
        contactInfo: String = "",
        productImageUrl: String = ""
    ) {
        // Check if this barcode already exists in the gallery
        if (barcodes.any { it.value == value }) {
            // Show alert for duplicate barcode instead of adding
            context?.let { ctx ->
                val dialog = Dialog(ctx)
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
                dialog.setContentView(R.layout.dialog_duplicate_barcode)
                dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

                // Set up dialog views
                val messageTextView = dialog.findViewById<TextView>(R.id.dialog_message)
                val barcodeValueTextView = dialog.findViewById<TextView>(R.id.barcode_value)
                val okButton = dialog.findViewById<Button>(R.id.btn_ok)

                // Set the message and barcode value
                messageTextView.text = "This barcode already exists in '$galleryName'."
                barcodeValueTextView.text = value

                // Set button listener
                okButton.setOnClickListener {
                    dialog.dismiss()
                }

                dialog.show()
            }
            return
        }

        // Create new barcode with all information
        val newBarcode = BarcodeData(
            value = value,
            format = format,
            galleryName = galleryName,
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

        // Add to the list
        barcodes.add(newBarcode)

        // Update the adapter
        barcodeAdapter.updateBarcodes(barcodes)

        // Save to SharedPreferences
        saveBarcodesToSharedPreferences()

        // Update visibility of barcodes section
        updateContentVisibility()

        // Show confirmation with proper product name if available
        val confirmationMessage = if (title.isNotEmpty()) {
            "Barcode added to $galleryName: $title"
        } else {
            "Barcode added to $galleryName"
        }

        Toast.makeText(context, confirmationMessage, Toast.LENGTH_SHORT).show()
    }

    private fun launchBarcodeScanner() {
        val intent = Intent(requireContext(), com.bar.honeypot.ui.scanner.BarcodeScannerActivity::class.java)
        barcodeScannerLauncher.launch(intent)
    }

    private fun showAddOptionsDialog() {
        context?.let { ctx ->
            // Create a custom dialog
            val dialog = Dialog(ctx)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(R.layout.dialog_add_gallery_item)

            // Make dialog background transparent to show rounded corners
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            // Set the gallery name in the title
            val titleText = dialog.findViewById<TextView>(R.id.dialog_title)
            titleText.text = "Add New Item to $galleryName"

            // Set up click listeners
            val closeButton = dialog.findViewById<ImageButton>(R.id.btn_close)
            val addSubgalleryOption = dialog.findViewById<View>(R.id.option_add_subgallery)
            val addBarcodeOption = dialog.findViewById<View>(R.id.option_add_barcode)
            val cancelButton = dialog.findViewById<Button>(R.id.btn_cancel)

            // Hide the sub-gallery option if this is already a sub-gallery
            if (isSubgallery) {
                addSubgalleryOption.visibility = View.GONE
            }

            // Close button
            closeButton.setOnClickListener {
                dialog.dismiss()
            }

            // Add subgallery option
            addSubgalleryOption.setOnClickListener {
                dialog.dismiss()
                showAddSubgalleryDialog()
            }

            // Add barcode option
            addBarcodeOption.setOnClickListener {
                dialog.dismiss()
                showBarcodeActionChoiceDialog()
            }

            // Cancel button
            cancelButton.setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()
        }
    }

    private fun showEditSubgalleryDialog(subgallery: GalleryItem, position: Int) {
        context?.let { ctx ->
            // Create a custom dialog
            val dialog = Dialog(ctx)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(R.layout.dialog_create_subgallery)

            // Make dialog background transparent to show rounded corners
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            // Get references to dialog views
            val nameInput = dialog.findViewById<EditText>(R.id.edit_subgallery_name)
            val closeButton = dialog.findViewById<ImageButton>(R.id.btn_close)
            val cancelButton = dialog.findViewById<Button>(R.id.btn_cancel)
            val createButton = dialog.findViewById<Button>(R.id.btn_create)
            val titleText = dialog.findViewById<TextView>(R.id.dialog_title)

            // Update UI for editing
            titleText.text = "Edit Sub-gallery"
            createButton.text = "UPDATE"
            nameInput.setText(subgallery.name)

            // Set click listeners
            closeButton.setOnClickListener { dialog.dismiss() }
            cancelButton.setOnClickListener { dialog.dismiss() }

            createButton.setOnClickListener {
                val newName = nameInput.text.toString().trim()

                when {
                    newName.isEmpty() -> {
                        nameInput.error = "Sub-gallery name cannot be empty"
                    }

                    newName != subgallery.name && subgalleries.any { it.name == newName } -> {
                        nameInput.error = "A sub-gallery with this name already exists"
                    }

                    else -> {
                        // Update sub-gallery
                        val updatedSubgallery = subgallery.copy(name = newName)
                        subgalleries[position] = updatedSubgallery

                        // Create a new list to force adapter update
                        val newList = subgalleries.toList()

                        // Update the adapter with the new list - clear first then submit
                        subgalleriesAdapter.submitList(null) // Clear the list first
                        Handler(Looper.getMainLooper()).postDelayed({
                            subgalleriesAdapter.submitList(newList) // Then submit the new list after delay
                        }, 50)

                        saveSubgalleriesToSharedPreferences()
                        dialog.dismiss()
                        Toast.makeText(ctx, "Sub-gallery updated", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            dialog.show()
        }
    }

    private fun showDeleteSubgalleryDialog(subgallery: GalleryItem, position: Int) {
        context?.let { ctx ->
            // Create a custom dialog
            val dialog = Dialog(ctx)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(R.layout.dialog_delete_subgallery)

            // Make dialog background transparent to show rounded corners
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            // Get references to dialog views
            val titleText = dialog.findViewById<TextView>(R.id.dialog_title)
            val messageText = dialog.findViewById<TextView>(R.id.dialog_message)
            val closeButton = dialog.findViewById<ImageButton>(R.id.btn_close)
            val cancelButton = dialog.findViewById<Button>(R.id.btn_cancel)
            val deleteButton = dialog.findViewById<Button>(R.id.btn_delete)

            // Set gallery name in the message
            messageText.text =
                "Are you sure you want to delete '${subgallery.name}'? This action cannot be undone."

            // Set click listeners
            closeButton.setOnClickListener { dialog.dismiss() }
            cancelButton.setOnClickListener { dialog.dismiss() }

            deleteButton.setOnClickListener {
                // Remove the sub-gallery
                subgalleries.removeAt(position)

                // Create a new list to force adapter update
                val newList = subgalleries.toList()

                // Update the adapter with the new list
                subgalleriesAdapter.submitList(null) // Clear the list first
                Handler(Looper.getMainLooper()).postDelayed({
                    subgalleriesAdapter.submitList(newList) // Then submit the new list after delay
                }, 50)

                saveSubgalleriesToSharedPreferences()
                updateContentVisibility()
                Toast.makeText(ctx, "Sub-gallery deleted", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }

            dialog.show()
        }
    }

    private fun showAddSubgalleryDialog() {
        context?.let { ctx ->
            // Create a custom dialog
            val dialog = Dialog(ctx)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(R.layout.dialog_create_subgallery)

            // Make dialog background transparent to show rounded corners
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            // Get references to dialog views
            val nameInput = dialog.findViewById<EditText>(R.id.edit_subgallery_name)
            val closeButton = dialog.findViewById<ImageButton>(R.id.btn_close)
            val cancelButton = dialog.findViewById<Button>(R.id.btn_cancel)
            val createButton = dialog.findViewById<Button>(R.id.btn_create)

            // Set click listeners
            closeButton.setOnClickListener { dialog.dismiss() }
            cancelButton.setOnClickListener { dialog.dismiss() }

            createButton.setOnClickListener {
                val subgalleryName = nameInput.text.toString().trim()

                when {
                    subgalleryName.isEmpty() -> {
                        nameInput.error = "Sub-gallery name cannot be empty"
                    }

                    subgalleries.any { it.name == subgalleryName } -> {
                        nameInput.error = "A sub-gallery with this name already exists"
                    }

                    else -> {
                        // Create new sub-gallery
                        createSubgallery(subgalleryName)
                        dialog.dismiss()

                        // Show confirmation
                        Toast.makeText(
                            context,
                            "Sub-gallery '$subgalleryName' created",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            dialog.show()
        }
    }

    private fun createSubgallery(name: String) {
        // Create a new sub-gallery with the given name
        val newSubgallery = GalleryItem(name, "0 items")

        // Add to the list
        subgalleries.add(newSubgallery)

        // Create a new list to force adapter update
        val newList = subgalleries.toList()

        // Update the adapter with the new list
        subgalleriesAdapter.submitList(null) // Clear the list first
        Handler(Looper.getMainLooper()).postDelayed({
            subgalleriesAdapter.submitList(newList) // Then submit the new list after delay
        }, 50)

        // Save to SharedPreferences
        saveSubgalleriesToSharedPreferences()

        // Update visibility of sub-galleries section
        updateContentVisibility()
    }

    private fun saveSubgalleriesToSharedPreferences() {
        val sharedPreferences =
            requireActivity().getSharedPreferences("HoneypotPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // Convert list to JSON
        val subgalleriesJson = Gson().toJson(subgalleries)

        // Save to SharedPreferences with the current gallery as the parent
        editor.putString("subgalleries_$galleryName", subgalleriesJson)
        editor.commit() // Use commit instead of apply for immediate saving
    }

    private fun showEnlargedBarcodeDialog(barcode: BarcodeData) {
        context?.let { ctx ->
            val dialog = Dialog(ctx, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
            dialog.setContentView(R.layout.dialog_enlarged_barcode)

            // Make sure the dialog takes up the full screen with no status bar
            dialog.window?.let { window ->
                window.setFlags(
                    android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN
                )
                window.decorView.systemUiVisibility = (
                        android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                                android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                                android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                                android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                                android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                                android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                        )
            }

            val enlargedImageView = dialog.findViewById<ImageView>(R.id.enlarged_barcode_image)
            val barcodeValueText = dialog.findViewById<TextView>(R.id.enlarged_barcode_value)
            val barcodeFormatText = dialog.findViewById<TextView>(R.id.enlarged_barcode_format)

            // Set barcode information
            barcodeValueText?.text = barcode.value
            barcodeFormatText?.text = "Format: ${barcode.format}"

            // Generate high-resolution barcode
            generateEnlargedBarcodeImage(barcode.value, barcode.format, enlargedImageView)

            // Track rotation and scaling state for the barcode image
            var isBarcodeRotated = false
            var currentScale = 1f
            val originalWidth =
                enlargedImageView?.layoutParams?.width ?: ViewGroup.LayoutParams.MATCH_PARENT
            val originalHeight =
                enlargedImageView?.layoutParams?.height ?: ViewGroup.LayoutParams.MATCH_PARENT

            // Get screen dimensions for auto-sizing
            val displayMetrics = DisplayMetrics()
            activity?.windowManager?.defaultDisplay?.getMetrics(displayMetrics)
            val screenWidth = displayMetrics.widthPixels
            val screenHeight = displayMetrics.heightPixels

            // Set up gesture detector for tap and long press handling
            val gestureDetector =
                GestureDetector(ctx, object : GestureDetector.SimpleOnGestureListener() {
                    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                        return true
                    }

                    override fun onDoubleTap(e: MotionEvent): Boolean {
                        if (isBarcodeRotated) {
                            // Animate back to normal rotation and reset scale
                            enlargedImageView?.animate()
                                ?.rotation(0f)
                                ?.scaleX(1f)
                                ?.scaleY(1f)
                                ?.setDuration(300)
                                ?.start()
                            currentScale = 1f
                            isBarcodeRotated = false

                            // Restore original size
                            enlargedImageView?.let { imageView ->
                                imageView.layoutParams.width = originalWidth
                                imageView.layoutParams.height = originalHeight
                                imageView.layoutParams = imageView.layoutParams
                            }
                        } else {
                            dialog.dismiss() // Close enlarged dialog
                            // Show the regular dialog again
                            showBarcodeDetails(
                                barcode,
                                -1
                            ) // Position -1 since we don't need it for display
                        }
                        return true
                    }

                    override fun onLongPress(e: MotionEvent) {
                        if (!isBarcodeRotated) {
                            // Calculate optimal size for landscape mode based on barcode type
                            val isLinearBarcode = when (barcode.format) {
                                "CODE_128", "CODE_39", "CODE_93", "EAN_13", "EAN_8", "UPC_A", "UPC_E", "CODABAR", "ITF" -> true
                                else -> false
                            }

                            val newWidth: Int
                            val newHeight: Int

                            if (isLinearBarcode) {
                                // For linear barcodes, make them wide and shorter
                                newWidth = (screenWidth * 0.9).toInt()  // 90% of screen width
                                newHeight = (screenHeight * 0.2).toInt() // 20% of screen height
                            } else {
                                // For 2D barcodes (QR, etc.), make them larger but square-ish
                                val maxSize =
                                    (kotlin.math.min(screenWidth, screenHeight) * 0.8).toInt()
                                newWidth = maxSize
                                newHeight = maxSize
                            }

                            // Animate rotation to 90 degrees and resize
                            enlargedImageView?.animate()
                                ?.rotation(90f)
                                ?.setDuration(300)
                                ?.start()

                            // Resize the ImageView
                            enlargedImageView?.let { imageView ->
                                imageView.layoutParams.width = newWidth
                                imageView.layoutParams.height = newHeight
                                imageView.layoutParams = imageView.layoutParams
                            }

                            isBarcodeRotated = true
                        }
                    }

                    override fun onDown(e: MotionEvent): Boolean {
                        return true // Must return true to receive subsequent events
                    }
                })

            // Set up pinch-to-zoom functionality
            val scaleGestureDetector = ScaleGestureDetector(
                ctx,
                object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    override fun onScale(detector: ScaleGestureDetector): Boolean {
                        val scaleFactor = detector.scaleFactor
                        currentScale *= scaleFactor

                        // Constrain scaling between 0.5x and 5x
                        currentScale = currentScale.coerceIn(0.5f, 5f)

                        enlargedImageView?.scaleX = currentScale
                        enlargedImageView?.scaleY = currentScale

                        return true
                    }
                })

            // Enhanced touch handling for zoom, tap, and long press

            enlargedImageView?.setOnTouchListener { _, event ->
                // Handle pinch-to-zoom first

                val scaleHandled = scaleGestureDetector.onTouchEvent(event)

                // Handle gestures only if not currently scaling

                if (!scaleGestureDetector.isInProgress) {

                    gestureDetector.onTouchEvent(event)
                }

                true // Consume the event
            }

            // Close dialog when tapped anywhere else (background)

            dialog.findViewById<View>(android.R.id.content)?.setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()
        }
    }

    private fun generateEnlargedBarcodeImage(value: String, format: String, imageView: ImageView?) {
        if (imageView == null) return

        try {
            val barcodeFormat = when (format) {
                "QR_CODE" -> BarcodeFormat.QR_CODE
                "CODE_128" -> BarcodeFormat.CODE_128
                "CODE_39" -> BarcodeFormat.CODE_39
                "CODE_93" -> BarcodeFormat.CODE_93
                "EAN_13" -> BarcodeFormat.EAN_13
                "EAN_8" -> BarcodeFormat.EAN_8
                "UPC_A" -> BarcodeFormat.UPC_A
                "UPC_E" -> BarcodeFormat.UPC_E
                "DATA_MATRIX" -> BarcodeFormat.DATA_MATRIX
                "AZTEC" -> BarcodeFormat.AZTEC
                "PDF417" -> BarcodeFormat.PDF_417
                "CODABAR" -> BarcodeFormat.CODABAR
                "ITF" -> BarcodeFormat.ITF
                else -> BarcodeFormat.QR_CODE
            }

            val writer = BarcodeEncoder()

            // Use high resolution for enlarged view

            val width = if (format == "CODE_128" && value.length > 20) 1200 else 800

            val height = if (format == "CODE_128" && value.length > 20) 300 else 800

            val bitmap = writer.encodeBitmap(value, barcodeFormat, width, height)

            imageView.setImageBitmap(bitmap)
        } catch (e: Exception) {
            Toast.makeText(context, "Error generating enlarged barcode", Toast.LENGTH_SHORT).show()
        }
    }

    // Photo handling functionality

    private var currentPhotoPath: String = ""
    private var currentBarcodeForPhoto: BarcodeData? = null

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && currentBarcodeForPhoto != null) {
            // Save the captured photo path to the barcode

            currentBarcodeForPhoto?.let { barcode ->
                // Update barcode with the new image path

                val updatedBarcode = barcode.copy(productImageUrl = currentPhotoPath)

                val index = barcodes.indexOf(barcode)

                if (index != -1) {
                    barcodes[index] = updatedBarcode

                    barcodeAdapter.updateBarcodes(barcodes)

                    saveBarcodesToSharedPreferences()

                    Toast.makeText(context, "Product photo saved", Toast.LENGTH_SHORT).show()

                    // Show the updated barcode

                    showBarcodeDetails(updatedBarcode, index)
                }
            }
        } else {
            // Clean up the failed photo file

            if (currentPhotoPath.isNotEmpty()) {
                File(currentPhotoPath).delete()
            }
        }

        currentPhotoPath = ""

        currentBarcodeForPhoto = null
    }

    @Throws(IOException::class)

    private fun createImageFile(): File {
        // Create an image file name

        val timeStamp: String =
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

        val storageDir: File? = context?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        return File.createTempFile(
            "BARCODE_${timeStamp}_", /* prefix */

            ".jpg", /* suffix */

            storageDir /* directory */

        ).apply {
            // Save a file: path for use with ACTION_VIEW intents

            currentPhotoPath = absolutePath
        }
    }

    private fun dispatchTakePictureIntent(barcode: BarcodeData) {
        try {
            val photoFile: File = createImageFile()

            val photoURI: Uri = FileProvider.getUriForFile(
                requireContext(),
                "com.bar.honeypot.fileprovider",
                photoFile
            )

            currentBarcodeForPhoto = barcode

            takePictureLauncher.launch(photoURI)
        } catch (ex: IOException) {
            Toast.makeText(context, "Error creating image file", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Show action bar again when fragment is destroyed

        (activity as? AppCompatActivity)?.supportActionBar?.show()
    }
}
