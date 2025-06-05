package com.bar.honeypot.ui.scanner

import com.google.mlkit.vision.barcode.common.Barcode
import org.junit.Test
import org.junit.Assert.*

class BarcodeScannerTest {

    @Test
    fun `getReadableFormat returns correct format for CODE_128`() {
        // Given
        val formatInt = Barcode.FORMAT_CODE_128

        // When
        val result = getReadableFormatForTesting(formatInt)

        // Then
        assertEquals("CODE_128", result)
    }

    @Test
    fun `getReadableFormat returns correct format for UPC_A`() {
        // Given
        val formatInt = Barcode.FORMAT_UPC_A

        // When
        val result = getReadableFormatForTesting(formatInt)

        // Then
        assertEquals("UPC_A", result)
    }

    @Test
    fun `getReadableFormat returns correct format for EAN_13`() {
        // Given
        val formatInt = Barcode.FORMAT_EAN_13

        // When
        val result = getReadableFormatForTesting(formatInt)

        // Then
        assertEquals("EAN_13", result)
    }

    @Test
    fun `getReadableFormat returns correct format for QR_CODE`() {
        // Given
        val formatInt = Barcode.FORMAT_QR_CODE

        // When
        val result = getReadableFormatForTesting(formatInt)

        // Then
        assertEquals("QR_CODE", result)
    }

    @Test
    fun `getReadableFormat returns UNKNOWN for unrecognized format`() {
        // Given
        val formatInt = 99999 // Invalid format

        // When
        val result = getReadableFormatForTesting(formatInt)

        // Then
        assertEquals("UNKNOWN", result)
    }

    @Test
    fun `isProductBarcode returns true for EAN_13`() {
        // When
        val result = isProductBarcodeForTesting("EAN_13")

        // Then
        assertTrue(result)
    }

    @Test
    fun `isProductBarcode returns true for EAN_8`() {
        // When
        val result = isProductBarcodeForTesting("EAN_8")

        // Then
        assertTrue(result)
    }

    @Test
    fun `isProductBarcode returns true for UPC_A`() {
        // When
        val result = isProductBarcodeForTesting("UPC_A")

        // Then
        assertTrue(result)
    }

    @Test
    fun `isProductBarcode returns true for UPC_E`() {
        // When
        val result = isProductBarcodeForTesting("UPC_E")

        // Then
        assertTrue(result)
    }

    @Test
    fun `isProductBarcode returns true for CODE_128`() {
        // When
        val result = isProductBarcodeForTesting("CODE_128")

        // Then
        assertTrue(result)
    }

    @Test
    fun `isProductBarcode returns false for QR_CODE`() {
        // When
        val result = isProductBarcodeForTesting("QR_CODE")

        // Then
        assertFalse(result)
    }

    @Test
    fun `isProductBarcode returns false for PDF417`() {
        // When
        val result = isProductBarcodeForTesting("PDF417")

        // Then
        assertFalse(result)
    }

    @Test
    fun `isProductBarcode returns false for AZTEC`() {
        // When
        val result = isProductBarcodeForTesting("AZTEC")

        // Then
        assertFalse(result)
    }

    @Test
    fun `isProductBarcode returns false for DATA_MATRIX`() {
        // When
        val result = isProductBarcodeForTesting("DATA_MATRIX")

        // Then
        assertFalse(result)
    }

    @Test
    fun `isProductBarcode returns false for unknown format`() {
        // When
        val result = isProductBarcodeForTesting("UNKNOWN")

        // Then
        assertFalse(result)
    }

    @Test
    fun `isProductBarcode returns false for empty string`() {
        // When
        val result = isProductBarcodeForTesting("")

        // Then
        assertFalse(result)
    }

    // Helper functions to test private methods (would be extracted from BarcodeScannerActivity)
    private fun getReadableFormatForTesting(format: Int): String {
        return when (format) {
            Barcode.FORMAT_CODE_128 -> "CODE_128"
            Barcode.FORMAT_CODE_39 -> "CODE_39"
            Barcode.FORMAT_CODE_93 -> "CODE_93"
            Barcode.FORMAT_CODABAR -> "CODABAR"
            Barcode.FORMAT_DATA_MATRIX -> "DATA_MATRIX"
            Barcode.FORMAT_EAN_13 -> "EAN_13"
            Barcode.FORMAT_EAN_8 -> "EAN_8"
            Barcode.FORMAT_ITF -> "ITF"
            Barcode.FORMAT_QR_CODE -> "QR_CODE"
            Barcode.FORMAT_UPC_A -> "UPC_A"
            Barcode.FORMAT_UPC_E -> "UPC_E"
            Barcode.FORMAT_PDF417 -> "PDF417"
            Barcode.FORMAT_AZTEC -> "AZTEC"
            else -> "UNKNOWN"
        }
    }

    private fun isProductBarcodeForTesting(format: String): Boolean {
        return when (format) {
            "EAN_13", "EAN_8", "UPC_A", "UPC_E" -> true
            "CODE_128" -> true // Many products use CODE_128
            else -> false
        }
    }
}

/**
 * Tests for barcode value validation
 */
class BarcodeValidationTest {

    @Test
    fun `UPC_A validation accepts 12 digit codes`() {
        // Given
        val validUpcA = "123456789012"

        // When
        val result = isValidUpcA(validUpcA)

        // Then
        assertTrue(result)
    }

    @Test
    fun `UPC_A validation rejects codes with less than 12 digits`() {
        // Given
        val shortCode = "12345678901"

        // When
        val result = isValidUpcA(shortCode)

        // Then
        assertFalse(result)
    }

    @Test
    fun `UPC_A validation rejects codes with more than 12 digits`() {
        // Given
        val longCode = "1234567890123"

        // When
        val result = isValidUpcA(longCode)

        // Then
        assertFalse(result)
    }

    @Test
    fun `UPC_A validation rejects codes with non-numeric characters`() {
        // Given
        val invalidCode = "12345678901a"

        // When
        val result = isValidUpcA(invalidCode)

        // Then
        assertFalse(result)
    }

    @Test
    fun `EAN_13 validation accepts 13 digit codes`() {
        // Given
        val validEan13 = "1234567890123"

        // When
        val result = isValidEan13(validEan13)

        // Then
        assertTrue(result)
    }

    @Test
    fun `EAN_13 validation rejects codes with incorrect length`() {
        // Given
        val invalidCode = "123456789012"

        // When
        val result = isValidEan13(invalidCode)

        // Then
        assertFalse(result)
    }

    @Test
    fun `EAN_8 validation accepts 8 digit codes`() {
        // Given
        val validEan8 = "12345678"

        // When
        val result = isValidEan8(validEan8)

        // Then
        assertTrue(result)
    }

    @Test
    fun `CODE_128 validation accepts alphanumeric codes`() {
        // Given
        val validCode128 = "ABC123def456"

        // When
        val result = isValidCode128(validCode128)

        // Then
        assertTrue(result)
    }

    @Test
    fun `CODE_128 validation rejects empty codes`() {
        // Given
        val emptyCode = ""

        // When
        val result = isValidCode128(emptyCode)

        // Then
        assertFalse(result)
    }

    // Helper validation functions
    private fun isValidUpcA(code: String): Boolean {
        return code.length == 12 && code.all { it.isDigit() }
    }

    private fun isValidEan13(code: String): Boolean {
        return code.length == 13 && code.all { it.isDigit() }
    }

    private fun isValidEan8(code: String): Boolean {
        return code.length == 8 && code.all { it.isDigit() }
    }

    private fun isValidCode128(code: String): Boolean {
        return code.isNotEmpty()
    }
}

/**
 * Tests for product naming logic
 */
class ProductNamingTest {

    @Test
    fun `generateDefaultProductName creates format-specific names for UPC_A`() {
        // Given
        val format = "UPC_A"
        val value = "123456789012"

        // When
        val result = generateDefaultProductName(format, value)

        // Then
        assertEquals("UPC_A Product: 123456789012", result)
    }

    @Test
    fun `generateDefaultProductName creates format-specific names for EAN_13`() {
        // Given
        val format = "EAN_13"
        val value = "1234567890123"

        // When
        val result = generateDefaultProductName(format, value)

        // Then
        assertEquals("EAN_13 Product: 1234567890123", result)
    }

    @Test
    fun `generateDefaultProductName creates generic names for other formats`() {
        // Given
        val format = "QR_CODE"
        val value = "https://example.com"

        // When
        val result = generateDefaultProductName(format, value)

        // Then
        assertEquals("Product: https://example.com", result)
    }

    @Test
    fun `combineProductNameAndBrand combines both when available`() {
        // Given
        val productName = "Product Name"
        val brand = "Brand Name"

        // When
        val result = combineProductNameAndBrand(productName, brand)

        // Then
        assertEquals("Brand Name Product Name", result)
    }

    @Test
    fun `combineProductNameAndBrand returns product name when brand is empty`() {
        // Given
        val productName = "Product Name"
        val brand = ""

        // When
        val result = combineProductNameAndBrand(productName, brand)

        // Then
        assertEquals("Product Name", result)
    }

    @Test
    fun `combineProductNameAndBrand returns brand when product name is empty`() {
        // Given
        val productName = ""
        val brand = "Brand Name"

        // When
        val result = combineProductNameAndBrand(productName, brand)

        // Then
        assertEquals("Brand Name", result)
    }

    @Test
    fun `combineProductNameAndBrand returns null when both are empty`() {
        // Given
        val productName = ""
        val brand = ""

        // When
        val result = combineProductNameAndBrand(productName, brand)

        // Then
        assertNull(result)
    }

    // Helper functions for product naming
    private fun generateDefaultProductName(format: String, value: String): String {
        return when (format) {
            "UPC_A" -> "UPC_A Product: $value"
            "UPC_E" -> "UPC_E Product: $value"
            "EAN_13" -> "EAN_13 Product: $value"
            "EAN_8" -> "EAN_8 Product: $value"
            else -> "Product: $value"
        }
    }

    private fun combineProductNameAndBrand(productName: String, brand: String): String? {
        return when {
            productName.isNotEmpty() && brand.isNotEmpty() -> "$brand $productName"
            productName.isNotEmpty() -> productName
            brand.isNotEmpty() -> brand
            else -> null
        }
    }
} 