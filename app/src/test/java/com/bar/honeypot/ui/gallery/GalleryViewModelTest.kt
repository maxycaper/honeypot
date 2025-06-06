package com.bar.honeypot.ui.gallery

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.bar.honeypot.model.BarcodeData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner

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
    fun `barcodes LiveData starts empty`() {
        // Observe the LiveData
        viewModel.barcodes.observeForever(barcodesObserver)

        // Verify that the observer received an empty list
        verify(barcodesObserver).onChanged(mutableListOf())

        // Clean up
        viewModel.barcodes.removeObserver(barcodesObserver)
    }

    @Test
    fun `gallery name can be set and observed`() {
        val testGalleryName = "Test Gallery"
        
        // Observe the LiveData
        viewModel.currentGalleryName.observeForever(galleryNameObserver)

        // Set the gallery name
        viewModel.setGalleryName(testGalleryName)

        // Verify that the observer received the expected value
        verify(galleryNameObserver).onChanged(testGalleryName)

        // Clean up
        viewModel.currentGalleryName.removeObserver(galleryNameObserver)
    }

    @Test
    fun `addBarcode adds barcode to list`() {
        val testValue = "123456789"
        val testFormat = "EAN_13"
        
        // Add a barcode
        val success = viewModel.addBarcode(testValue, testFormat)
        
        // Verify the barcode was added successfully
        assertTrue(success)
        assertEquals(1, viewModel.barcodes.value?.size)
        assertEquals(testValue, viewModel.barcodes.value?.get(0)?.value)
        assertEquals(testFormat, viewModel.barcodes.value?.get(0)?.format)
    }

    @Test
    fun `isDuplicateBarcode returns true for existing barcode`() {
        val testValue = "123456789"
        val testFormat = "EAN_13"
        
        // Add a barcode
        viewModel.addBarcode(testValue, testFormat)
        
        // Check if it's a duplicate
        assertTrue(viewModel.isDuplicateBarcode(testValue))
    }

    @Test
    fun `isDuplicateBarcode returns false for non-existing barcode`() {
        // Check if a non-existing barcode is a duplicate
        assertFalse(viewModel.isDuplicateBarcode("non-existing"))
    }

    @Test
    fun `addBarcode prevents duplicate barcodes`() {
        val testValue = "123456789"
        val testFormat = "EAN_13"
        
        // Add a barcode twice
        val firstAdd = viewModel.addBarcode(testValue, testFormat)
        val secondAdd = viewModel.addBarcode(testValue, testFormat)
        
        // Verify first add succeeded, second failed
        assertTrue(firstAdd)
        assertFalse(secondAdd)
        assertEquals(1, viewModel.barcodes.value?.size)
    }
} 