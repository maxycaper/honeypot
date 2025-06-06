package com.bar.honeypot.ui.gallery

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bar.honeypot.R
import com.bar.honeypot.model.BarcodeData

/**
 * Simple adapter - tap item container to show dialog
 */
class BarcodeAdapter(
    private val context: Context,
    private val onItemClick: (BarcodeData, Int) -> Unit
) : RecyclerView.Adapter<BarcodeAdapter.BarcodeViewHolder>() {

    companion object {
        private const val TAG = "BarcodeAdapter"
    }

    private var barcodes = mutableListOf<BarcodeData>()

    /**
     * Simple ViewHolder - just handles item clicks
     */
    inner class BarcodeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.barcode_title)
        private val valueTextView: TextView = itemView.findViewById(R.id.barcode_value)
        private val formatTextView: TextView = itemView.findViewById(R.id.barcode_format)

        fun bind(barcode: BarcodeData, position: Int) {
            Log.d(TAG, "Binding barcode at position $position: '${barcode.value}' (${barcode.format})")
            
            titleTextView.text = barcode.title.ifEmpty { "Barcode ${position + 1}" }
            valueTextView.text = barcode.value
            formatTextView.text = barcode.format
            
            // Simple click on barcode_item_container to show dialog
            itemView.setOnClickListener {
                Log.i(TAG, "✅ Click on barcode_item_container at position $position: '${barcode.value}'")
                onItemClick(barcode, position)
            }
        }


    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BarcodeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_barcode, parent, false)
        return BarcodeViewHolder(view)
    }

    override fun onBindViewHolder(holder: BarcodeViewHolder, position: Int) {
        holder.bind(barcodes[position], position)
    }

    override fun getItemCount(): Int = barcodes.size

    fun updateBarcodes(newBarcodes: List<BarcodeData>) {
        Log.i(TAG, "Updating barcodes list with ${newBarcodes.size} items")
        barcodes.clear()
        barcodes.addAll(newBarcodes)
        notifyDataSetChanged()
    }

    fun removeBarcode(position: Int) {
        if (position in 0 until barcodes.size) {
            val removedBarcode = barcodes.removeAt(position)
            notifyItemRemoved(position)
            Log.i(TAG, "Removed barcode at position $position: '${removedBarcode.value}'")
        }
    }
} 