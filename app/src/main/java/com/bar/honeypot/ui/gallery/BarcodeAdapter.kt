package com.bar.honeypot.ui.gallery

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bar.honeypot.R
import com.bar.honeypot.model.BarcodeData

class BarcodeAdapter(private val onItemClick: (BarcodeData, Int) -> Unit) : 
    RecyclerView.Adapter<BarcodeAdapter.BarcodeViewHolder>() {
    
    private var barcodes: List<BarcodeData> = emptyList()
    
    fun submitList(newList: List<BarcodeData>) {
        barcodes = newList
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BarcodeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_barcode, parent, false)
        return BarcodeViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: BarcodeViewHolder, position: Int) {
        val barcode = barcodes[position]
        holder.bind(barcode)
        
        // Set content description and tag for easier UI testing/automation
        holder.itemView.contentDescription = "barcode_item_${barcode.value}"
        holder.itemView.tag = "barcode_item_${position}"
        
        holder.itemView.setOnClickListener {
            onItemClick(barcode, position)
        }
    }
    
    override fun getItemCount(): Int = barcodes.size
    
    class BarcodeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.barcode_title)
        private val valueTextView: TextView = itemView.findViewById(R.id.barcode_value)
        private val formatTextView: TextView = itemView.findViewById(R.id.barcode_format)
        
        fun bind(barcode: BarcodeData) {
            // Determine the title to display
            val displayTitle = when {
                barcode.productName.isNotEmpty() && !barcode.productName.startsWith("Product:") -> barcode.productName
                barcode.title.isNotEmpty() -> barcode.title
                barcode.url.isNotEmpty() -> "URL Barcode"
                barcode.email.isNotEmpty() -> "Email Barcode"
                barcode.phone.isNotEmpty() -> "Phone Barcode"
                barcode.wifiSsid.isNotEmpty() -> "WiFi Network"
                barcode.contactInfo.isNotEmpty() -> "Contact Information"
                barcode.geoLat != 0.0 && barcode.geoLng != 0.0 -> "Location"
                else -> "Barcode: ${barcode.format}"
            }
            
            titleTextView.text = displayTitle
            valueTextView.text = barcode.value
            formatTextView.text = "Format: ${barcode.format}"
            
            // Set content descriptions for accessibility and UI testing
            titleTextView.contentDescription = "title_${barcode.value}"
            valueTextView.contentDescription = "value_${barcode.value}"
            formatTextView.contentDescription = "format_${barcode.format}"
        }
    }
} 