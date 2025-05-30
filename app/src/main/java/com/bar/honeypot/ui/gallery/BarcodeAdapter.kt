package com.bar.honeypot.ui.gallery

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bar.honeypot.R
import com.bar.honeypot.model.BarcodeData

class BarcodeAdapter : ListAdapter<BarcodeData, BarcodeAdapter.BarcodeViewHolder>(BarcodeDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BarcodeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_barcode, parent, false)
        return BarcodeViewHolder(view)
    }

    override fun onBindViewHolder(holder: BarcodeViewHolder, position: Int) {
        val barcode = getItem(position)
        holder.bind(barcode)
    }

    class BarcodeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvBarcodeValue: TextView = itemView.findViewById(R.id.tv_barcode_value)
        private val tvBarcodeFormat: TextView = itemView.findViewById(R.id.tv_barcode_format)
        private val tvTimestamp: TextView = itemView.findViewById(R.id.tv_timestamp)

        fun bind(barcode: BarcodeData) {
            tvBarcodeValue.text = barcode.value
            tvBarcodeFormat.text = barcode.format
            tvTimestamp.text = barcode.getFormattedTimestamp()
        }
    }

    private class BarcodeDiffCallback : DiffUtil.ItemCallback<BarcodeData>() {
        override fun areItemsTheSame(oldItem: BarcodeData, newItem: BarcodeData): Boolean {
            return oldItem.value == newItem.value && oldItem.timestamp == newItem.timestamp
        }

        override fun areContentsTheSame(oldItem: BarcodeData, newItem: BarcodeData): Boolean {
            return oldItem == newItem
        }
    }
} 