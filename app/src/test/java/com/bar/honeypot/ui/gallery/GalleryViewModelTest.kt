package com.bar.honeypot.ui.gallery

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.bar.honeypot.model.BarcodeData
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import org.junit.Assert.*

@RunWith(MockitoJUnitRunner::class)
class GalleryViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var barcodesObserver: Observer<MutableList<BarcodeData>>

    @Mock
    private lateinit var galleryNameObserver: Observer<String>

    private lateinit var viewModel: GalleryViewModel

    @Before
    fun setup() {
        viewModel = GalleryViewModel()
    }

    @Test
    fun `init creates empty barcode list`() {
        // Given
        viewModel.barcodes.observeForever(barcodesObserver)

        // Then
        verify(barcodesObserver).onChanged(mutableListOf())
        assertEquals(0, viewModel.barcodes.value?.size)
    }

    @Test
    fun `setGalleryName updates gallery name`() {
        // Given
        val galleryName = "Test Gallery"
        viewModel.currentGalleryName.observeForever(galleryNameObserver)

        // When
        viewModel.setGalleryName(galleryName)

        // Then
        verify(galleryNameObserver).onChanged(galleryName)
        assertEquals(galleryName, viewModel.currentGalleryName.value)
    }

    @Test
    fun `addBarcode successfully adds new barcode`() {
        // Given
        viewModel.setGalleryName("Test Gallery")
        viewModel.barcodes.observeForever(barcodesObserver)
        val barcodeValue = "123456789012"
        val barcodeFormat = "UPC_A"

        // When
        val result = viewModel.addBarcode(
            value = barcodeValue,
            format = barcodeFormat,
            title = "Test Product"
        )

        // Then
        assertTrue(result)
        assertEquals(1, viewModel.barcodes.value?.size)
        assertEquals(barcodeValue, viewModel.barcodes.value?.first()?.value)
        assertEquals(barcodeFormat, viewModel.barcodes.value?.first()?.format)
        assertEquals("Test Product", viewModel.barcodes.value?.first()?.title)
    }

    @Test
    fun `addBarcode with product information saves all data`() {
        // Given
        viewModel.setGalleryName("Test Gallery")
        val barcodeValue = "1234567890123"
        val barcodeFormat = "EAN_13"

        // When
        val result = viewModel.addBarcode(
            value = barcodeValue,
            format = barcodeFormat,
            title = "Test Product",
            description = "Product description",
            productName = "Brand Product Name",
            productImageUrl = "https://example.com/image.jpg",
            url = "https://example.com"
        )

        // Then
        assertTrue(result)
        val savedBarcode = viewModel.barcodes.value?.first()
        assertNotNull(savedBarcode)
        assertEquals(barcodeValue, savedBarcode?.value)
        assertEquals(barcodeFormat, savedBarcode?.format)
        assertEquals("Test Product", savedBarcode?.title)
        assertEquals("Product description", savedBarcode?.description)
        assertEquals("Brand Product Name", savedBarcode?.productName)
        assertEquals("https://example.com/image.jpg", savedBarcode?.productImageUrl)
        assertEquals("https://example.com", savedBarcode?.url)
    }

    @Test
    fun `addBarcode rejects duplicate barcode`() {
        // Given
        viewModel.setGalleryName("Test Gallery")
        val barcodeValue = "123456789012"
        viewModel.addBarcode(value = barcodeValue, format = "UPC_A")

        // When
        val result = viewModel.addBarcode(value = barcodeValue, format = "EAN_13") // Different format, same value

        // Then
        assertFalse(result)
        assertEquals(1, viewModel.barcodes.value?.size) // Still only one barcode
    }

    @Test
    fun `isDuplicateBarcode returns true for existing barcode`() {
        // Given
        val barcodeValue = "123456789012"
        viewModel.addBarcode(value = barcodeValue, format = "UPC_A")

        // When
        val isDuplicate = viewModel.isDuplicateBarcode(barcodeValue)

        // Then
        assertTrue(isDuplicate)
    }

    @Test
    fun `isDuplicateBarcode returns false for new barcode`() {
        // Given
        viewModel.addBarcode(value = "123456789012", format = "UPC_A")

        // When
        val isDuplicate = viewModel.isDuplicateBarcode("987654321098")

        // Then
        assertFalse(isDuplicate)
    }

    @Test
    fun `isDuplicateBarcode returns false for empty list`() {
        // When
        val isDuplicate = viewModel.isDuplicateBarcode("123456789012")

        // Then
        assertFalse(isDuplicate)
    }

    @Test
    fun `deleteBarcode removes barcode at valid position`() {
        // Given
        viewModel.addBarcode(value = "123456789012", format = "UPC_A", title = "First")
        viewModel.addBarcode(value = "987654321098", format = "EAN_13", title = "Second")
        assertEquals(2, viewModel.barcodes.value?.size)

        // When
        viewModel.deleteBarcode(0) // Delete first barcode

        // Then
        assertEquals(1, viewModel.barcodes.value?.size)
        assertEquals("987654321098", viewModel.barcodes.value?.first()?.value)
        assertEquals("Second", viewModel.barcodes.value?.first()?.title)
    }

    @Test
    fun `deleteBarcode ignores invalid position`() {
        // Given
        viewModel.addBarcode(value = "123456789012", format = "UPC_A")
        val originalSize = viewModel.barcodes.value?.size

        // When
        viewModel.deleteBarcode(10) // Invalid position

        // Then
        assertEquals(originalSize, viewModel.barcodes.value?.size)
    }

    @Test
    fun `deleteBarcode ignores negative position`() {
        // Given
        viewModel.addBarcode(value = "123456789012", format = "UPC_A")
        val originalSize = viewModel.barcodes.value?.size

        // When
        viewModel.deleteBarcode(-1) // Negative position

        // Then
        assertEquals(originalSize, viewModel.barcodes.value?.size)
    }

    @Test
    fun `updateBarcodeTitle updates title at valid position`() {
        // Given
        viewModel.addBarcode(value = "123456789012", format = "UPC_A", title = "Original Title")
        val newTitle = "Updated Title"

        // When
        val result = viewModel.updateBarcodeTitle(0, newTitle)

        // Then
        assertTrue(result)
        assertEquals(newTitle, viewModel.barcodes.value?.first()?.title)
    }

    @Test
    fun `updateBarcodeTitle returns false for invalid position`() {
        // Given
        viewModel.addBarcode(value = "123456789012", format = "UPC_A", title = "Original Title")

        // When
        val result = viewModel.updateBarcodeTitle(10, "New Title")

        // Then
        assertFalse(result)
        assertEquals("Original Title", viewModel.barcodes.value?.first()?.title)
    }

    @Test
    fun `saveBarcodes calls callback with JSON`() {
        // Given
        viewModel.addBarcode(value = "123456789012", format = "UPC_A", title = "Test Product")
        var savedJson: String? = null

        // When
        viewModel.saveBarcodes { json ->
            savedJson = json
        }

        // Then
        assertNotNull(savedJson)
        assertTrue(savedJson!!.contains("123456789012"))
        assertTrue(savedJson!!.contains("UPC_A"))
        assertTrue(savedJson!!.contains("Test Product"))
    }

    @Test
    fun `loadBarcodes with valid JSON loads barcodes`() {
        // Given
        val galleryName = "Test Gallery"
        val jsonData = """[{"value":"123456789012","format":"UPC_A","galleryName":"Test Gallery","title":"Test Product","description":"","url":"","email":"","phone":"","smsContent":"","wifiSsid":"","wifiPassword":"","wifiType":"","geoLat":0.0,"geoLng":0.0,"productName":"","contactInfo":"","productImageUrl":""}]"""

        // When
        viewModel.loadBarcodes(galleryName, jsonData)

        // Then
        assertEquals(galleryName, viewModel.currentGalleryName.value)
        assertEquals(1, viewModel.barcodes.value?.size)
        assertEquals("123456789012", viewModel.barcodes.value?.first()?.value)
        assertEquals("UPC_A", viewModel.barcodes.value?.first()?.format)
        assertEquals("Test Product", viewModel.barcodes.value?.first()?.title)
    }

    @Test
    fun `loadBarcodes with empty JSON creates empty list`() {
        // Given
        val galleryName = "Test Gallery"

        // When
        viewModel.loadBarcodes(galleryName, "")

        // Then
        assertEquals(galleryName, viewModel.currentGalleryName.value)
        assertEquals(0, viewModel.barcodes.value?.size)
    }

    @Test
    fun `loadBarcodes with null JSON creates empty list`() {
        // Given
        val galleryName = "Test Gallery"

        // When
        viewModel.loadBarcodes(galleryName, null)

        // Then
        assertEquals(galleryName, viewModel.currentGalleryName.value)
        assertEquals(0, viewModel.barcodes.value?.size)
    }

    @Test
    fun `loadBarcodes with invalid JSON creates empty list`() {
        // Given
        val galleryName = "Test Gallery"
        val invalidJson = "invalid json"

        // When
        viewModel.loadBarcodes(galleryName, invalidJson)

        // Then
        assertEquals(galleryName, viewModel.currentGalleryName.value)
        assertEquals(0, viewModel.barcodes.value?.size)
    }

    @Test
    fun `loadBarcodes filters barcodes for specific gallery`() {
        // Given
        val galleryName = "Gallery 1"
        val jsonData = """[
            {"value":"123456789012","format":"UPC_A","galleryName":"Gallery 1","title":"Product 1","description":"","url":"","email":"","phone":"","smsContent":"","wifiSsid":"","wifiPassword":"","wifiType":"","geoLat":0.0,"geoLng":0.0,"productName":"","contactInfo":"","productImageUrl":""},
            {"value":"987654321098","format":"EAN_13","galleryName":"Gallery 2","title":"Product 2","description":"","url":"","email":"","phone":"","smsContent":"","wifiSsid":"","wifiPassword":"","wifiType":"","geoLat":0.0,"geoLng":0.0,"productName":"","contactInfo":"","productImageUrl":""},
            {"value":"555666777888","format":"CODE_128","galleryName":"Gallery 1","title":"Product 3","description":"","url":"","email":"","phone":"","smsContent":"","wifiSsid":"","wifiPassword":"","wifiType":"","geoLat":0.0,"geoLng":0.0,"productName":"","contactInfo":"","productImageUrl":""}
        ]"""

        // When
        viewModel.loadBarcodes(galleryName, jsonData)

        // Then
        assertEquals(2, viewModel.barcodes.value?.size) // Only barcodes from Gallery 1
        assertEquals("123456789012", viewModel.barcodes.value?.get(0)?.value)
        assertEquals("555666777888", viewModel.barcodes.value?.get(1)?.value)
    }

    @Test
    fun `addBarcode with WiFi data saves WiFi information`() {
        // Given
        viewModel.setGalleryName("Test Gallery")

        // When
        val result = viewModel.addBarcode(
            value = "WIFI:T:WPA;S:TestNetwork;P:password123;H:false;;",
            format = "QR_CODE",
            title = "WiFi QR Code",
            wifiSsid = "TestNetwork",
            wifiPassword = "password123",
            wifiType = "WPA"
        )

        // Then
        assertTrue(result)
        val savedBarcode = viewModel.barcodes.value?.first()
        assertEquals("TestNetwork", savedBarcode?.wifiSsid)
        assertEquals("password123", savedBarcode?.wifiPassword)
        assertEquals("WPA", savedBarcode?.wifiType)
    }

    @Test
    fun `addBarcode with contact data saves contact information`() {
        // Given
        viewModel.setGalleryName("Test Gallery")

        // When
        val result = viewModel.addBarcode(
            value = "BEGIN:VCARD...",
            format = "QR_CODE",
            title = "Contact QR Code",
            contactInfo = "John Doe\nPhone: 123-456-7890\nEmail: john@example.com"
        )

        // Then
        assertTrue(result)
        val savedBarcode = viewModel.barcodes.value?.first()
        assertEquals("John Doe\nPhone: 123-456-7890\nEmail: john@example.com", savedBarcode?.contactInfo)
    }

    @Test
    fun `addBarcode with geo coordinates saves location data`() {
        // Given
        viewModel.setGalleryName("Test Gallery")

        // When
        val result = viewModel.addBarcode(
            value = "geo:37.7749,-122.4194",
            format = "QR_CODE",
            title = "Location QR Code",
            geoLat = 37.7749,
            geoLng = -122.4194
        )

        // Then
        assertTrue(result)
        val savedBarcode = viewModel.barcodes.value?.first()
        assertEquals(37.7749, savedBarcode?.geoLat, 0.0001)
        assertEquals(-122.4194, savedBarcode?.geoLng, 0.0001)
    }
} 