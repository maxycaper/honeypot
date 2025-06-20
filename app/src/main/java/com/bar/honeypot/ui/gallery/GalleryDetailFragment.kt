package com.bar.honeypot.ui.gallery

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bar.honeypot.R
import com.bar.honeypot.databinding.FragmentGalleryDetailBinding
import com.bar.honeypot.model.BarcodeData
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class GalleryDetailFragment : Fragment() {

    private var _binding: FragmentGalleryDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var galleryName: String
    private lateinit var subgalleriesAdapter: GalleryListAdapter
    private lateinit var barcodeAdapter: BarcodeAdapter

    private val subgalleries = mutableListOf<GalleryItem>()
    private val barcodes = mutableListOf<BarcodeData>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGalleryDetailBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Get gallery name from arguments
        arguments?.let {
            galleryName = it.getString("gallery_name", "Gallery")
        }

        // Set the gallery name in the toolbar title
        (activity as? AppCompatActivity)?.supportActionBar?.title = galleryName

        // Setup RecyclerViews
        setupSubgalleriesRecyclerView()
        setupBarcodesRecyclerView()

        // Setup FAB
        binding.fabAdd.setOnClickListener {
            showAddOptionsDialog()
        }

        // Load data
        loadGalleryContent()

        return root
    }

    private fun setupSubgalleriesRecyclerView() {
        subgalleriesAdapter = GalleryListAdapter(
            onItemClick = { galleryItem ->
                // Handle regular click
                Toast.makeText(context, "Clicked: ${galleryItem.name}", Toast.LENGTH_SHORT).show()
            },
            onArrowClick = { galleryItem ->
                // Navigate to nested gallery
                navigateToSubgallery(galleryItem.name)
            }
        )

        binding.recyclerViewSubgalleries.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = subgalleriesAdapter
        }
    }

    private fun setupBarcodesRecyclerView() {
        barcodeAdapter = BarcodeAdapter(
            context = requireContext(),
            onItemClick = { barcode, position ->
                // Show barcode details
                showBarcodeDetails(barcode, position)
            }
        )

        binding.recyclerViewBarcodes.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = barcodeAdapter
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
                subgalleriesAdapter.submitList(subgalleries)
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
        // Show/hide subgalleries section
        if (subgalleries.isEmpty()) {
            binding.subgalleriesLabel.visibility = View.GONE
            binding.recyclerViewSubgalleries.visibility = View.GONE
        } else {
            binding.subgalleriesLabel.visibility = View.VISIBLE
            binding.recyclerViewSubgalleries.visibility = View.VISIBLE
        }

        // Show/hide barcodes section
        if (barcodes.isEmpty()) {
            binding.barcodesLabel.visibility = View.GONE
            binding.recyclerViewBarcodes.visibility = View.GONE
            binding.emptyView.visibility = View.VISIBLE
        } else {
            binding.barcodesLabel.visibility = View.VISIBLE
            binding.recyclerViewBarcodes.visibility = View.VISIBLE
            binding.emptyView.visibility = View.GONE
        }

        // If both are empty, show empty view
        if (subgalleries.isEmpty() && barcodes.isEmpty()) {
            binding.emptyView.visibility = View.VISIBLE
        }
    }

    private fun navigateToSubgallery(subgalleryName: String) {
        // Create a new instance of GalleryDetailFragment with the subgallery name
        val fragment = GalleryDetailFragment().apply {
            arguments = Bundle().apply {
                putString("gallery_name", subgalleryName)
            }
        }

        // Replace the current fragment with the new one
        parentFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment_content_main, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun showBarcodeDetails(barcode: BarcodeData, position: Int) {
        // We'll reuse the existing showBarcodeDisplayDialog from GalleryFragment
        // For now, this is just a placeholder
    }

    private fun showAddOptionsDialog() {
        // Show options to add new subgallery or barcode
        // This will be implemented later
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
