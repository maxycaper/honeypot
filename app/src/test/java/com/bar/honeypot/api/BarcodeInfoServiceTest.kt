package com.bar.honeypot.api

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever
import org.junit.Assert.*
import org.junit.Before

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class BarcodeInfoServiceTest {

    @Mock
    private lateinit var mockProductApiService: ProductApiService

    @Before
    fun setup() {
        // Mock the ProductApi.service
        mockedStatic(ProductApi::class.java).use { productApiMock ->
            productApiMock.`when`<ProductApiService> { ProductApi.service }.thenReturn(mockProductApiService)
        }
    }

    @Test
    fun `getProductInfo returns valid ProductInfo for successful API response`() = runTest {
        // Given
        val barcode = "123456789012"
        val mockProduct = Product(
            productName = "Test Product",
            productNameEn = "Test Product EN",
            genericName = null,
            genericNameEn = null,
            brands = "Test Brand",
            imageUrl = "https://example.com/image.jpg",
            imageFrontUrl = null
        )
        val mockResponse = ProductResponse(
            code = barcode,
            status = 1,
            statusVerbose = "product found",
            product = mockProduct
        )

        whenever(mockProductApiService.getProductInfoSuspend(barcode)).thenReturn(mockResponse)

        // When
        val result = BarcodeInfoService.getProductInfo(barcode)

        // Then
        assertNotNull(result)
        assertEquals(barcode, result!!.code)
        assertEquals("Test Product", result.name)
        assertEquals("Test Brand", result.brand)
        assertEquals("https://example.com/image.jpg", result.imageUrl)
    }

    @Test
    fun `getProductInfo returns ProductInfo with alternative name fields`() = runTest {
        // Given
        val barcode = "123456789012"
        val mockProduct = Product(
            productName = null,
            productNameEn = "English Product Name",
            genericName = "Generic Name",
            genericNameEn = null,
            brands = "Test Brand",
            imageUrl = null,
            imageFrontUrl = "https://example.com/front.jpg"
        )
        val mockResponse = ProductResponse(
            code = barcode,
            status = 1,
            statusVerbose = "product found",
            product = mockProduct
        )

        whenever(mockProductApiService.getProductInfoSuspend(barcode)).thenReturn(mockResponse)

        // When
        val result = BarcodeInfoService.getProductInfo(barcode)

        // Then
        assertNotNull(result)
        assertEquals("English Product Name", result!!.name)
        assertEquals("Test Brand", result.brand)
        assertEquals("https://example.com/front.jpg", result.imageUrl)
    }

    @Test
    fun `getProductInfo returns ProductInfo with genericName when other names are null`() = runTest {
        // Given
        val barcode = "123456789012"
        val mockProduct = Product(
            productName = null,
            productNameEn = null,
            genericName = "Generic Product",
            genericNameEn = null,
            brands = "Test Brand",
            imageUrl = "https://example.com/image.jpg",
            imageFrontUrl = null
        )
        val mockResponse = ProductResponse(
            code = barcode,
            status = 1,
            statusVerbose = "product found",
            product = mockProduct
        )

        whenever(mockProductApiService.getProductInfoSuspend(barcode)).thenReturn(mockResponse)

        // When
        val result = BarcodeInfoService.getProductInfo(barcode)

        // Then
        assertNotNull(result)
        assertEquals("Generic Product", result!!.name)
    }

    @Test
    fun `getProductInfo returns ProductInfo with genericNameEn as fallback`() = runTest {
        // Given
        val barcode = "123456789012"
        val mockProduct = Product(
            productName = null,
            productNameEn = null,
            genericName = null,
            genericNameEn = "Generic English Name",
            brands = "Test Brand",
            imageUrl = "https://example.com/image.jpg",
            imageFrontUrl = null
        )
        val mockResponse = ProductResponse(
            code = barcode,
            status = 1,
            statusVerbose = "product found",
            product = mockProduct
        )

        whenever(mockProductApiService.getProductInfoSuspend(barcode)).thenReturn(mockResponse)

        // When
        val result = BarcodeInfoService.getProductInfo(barcode)

        // Then
        assertNotNull(result)
        assertEquals("Generic English Name", result!!.name)
    }

    @Test
    fun `getProductInfo returns null when product is null`() = runTest {
        // Given
        val barcode = "123456789012"
        val mockResponse = ProductResponse(
            code = barcode,
            status = 0,
            statusVerbose = "product not found",
            product = null
        )

        whenever(mockProductApiService.getProductInfoSuspend(barcode)).thenReturn(mockResponse)

        // When
        val result = BarcodeInfoService.getProductInfo(barcode)

        // Then
        assertNull(result)
    }

    @Test
    fun `getProductInfo returns null when both name and brand are empty`() = runTest {
        // Given
        val barcode = "123456789012"
        val mockProduct = Product(
            productName = "",
            productNameEn = "",
            genericName = "",
            genericNameEn = "",
            brands = "",
            imageUrl = "",
            imageFrontUrl = ""
        )
        val mockResponse = ProductResponse(
            code = barcode,
            status = 1,
            statusVerbose = "product found",
            product = mockProduct
        )

        whenever(mockProductApiService.getProductInfoSuspend(barcode)).thenReturn(mockResponse)

        // When
        val result = BarcodeInfoService.getProductInfo(barcode)

        // Then
        assertNull(result)
    }

    @Test
    fun `getProductInfo returns ProductInfo when only brand is available`() = runTest {
        // Given
        val barcode = "123456789012"
        val mockProduct = Product(
            productName = "",
            productNameEn = "",
            genericName = "",
            genericNameEn = "",
            brands = "Only Brand Available",
            imageUrl = "https://example.com/image.jpg",
            imageFrontUrl = null
        )
        val mockResponse = ProductResponse(
            code = barcode,
            status = 1,
            statusVerbose = "product found",
            product = mockProduct
        )

        whenever(mockProductApiService.getProductInfoSuspend(barcode)).thenReturn(mockResponse)

        // When
        val result = BarcodeInfoService.getProductInfo(barcode)

        // Then
        assertNotNull(result)
        assertEquals("", result!!.name)
        assertEquals("Only Brand Available", result.brand)
    }

    @Test
    fun `getProductInfo returns null when API throws exception`() = runTest {
        // Given
        val barcode = "123456789012"
        whenever(mockProductApiService.getProductInfoSuspend(barcode))
            .thenThrow(RuntimeException("Network error"))

        // When
        val result = BarcodeInfoService.getProductInfo(barcode)

        // Then
        assertNull(result)
    }

    @Test
    fun `getProductInfo handles null values in product fields`() = runTest {
        // Given
        val barcode = "123456789012"
        val mockProduct = Product(
            productName = "Valid Name",
            productNameEn = null,
            genericName = null,
            genericNameEn = null,
            brands = null,
            imageUrl = null,
            imageFrontUrl = null
        )
        val mockResponse = ProductResponse(
            code = barcode,
            status = 1,
            statusVerbose = "product found",
            product = mockProduct
        )

        whenever(mockProductApiService.getProductInfoSuspend(barcode)).thenReturn(mockResponse)

        // When
        val result = BarcodeInfoService.getProductInfo(barcode)

        // Then
        assertNotNull(result)
        assertEquals("Valid Name", result!!.name)
        assertEquals("", result.brand)
        assertEquals("", result.imageUrl)
    }

    @Test
    fun `getProductInfo prefers productName over other name fields`() = runTest {
        // Given
        val barcode = "123456789012"
        val mockProduct = Product(
            productName = "Primary Name",
            productNameEn = "English Name",
            genericName = "Generic Name",
            genericNameEn = "Generic English Name",
            brands = "Test Brand",
            imageUrl = "https://example.com/image.jpg",
            imageFrontUrl = null
        )
        val mockResponse = ProductResponse(
            code = barcode,
            status = 1,
            statusVerbose = "product found",
            product = mockProduct
        )

        whenever(mockProductApiService.getProductInfoSuspend(barcode)).thenReturn(mockResponse)

        // When
        val result = BarcodeInfoService.getProductInfo(barcode)

        // Then
        assertNotNull(result)
        assertEquals("Primary Name", result!!.name) // Should prefer productName
    }

    @Test
    fun `getProductInfo prefers imageUrl over imageFrontUrl`() = runTest {
        // Given
        val barcode = "123456789012"
        val mockProduct = Product(
            productName = "Test Product",
            productNameEn = null,
            genericName = null,
            genericNameEn = null,
            brands = "Test Brand",
            imageUrl = "https://example.com/main.jpg",
            imageFrontUrl = "https://example.com/front.jpg"
        )
        val mockResponse = ProductResponse(
            code = barcode,
            status = 1,
            statusVerbose = "product found",
            product = mockProduct
        )

        whenever(mockProductApiService.getProductInfoSuspend(barcode)).thenReturn(mockResponse)

        // When
        val result = BarcodeInfoService.getProductInfo(barcode)

        // Then
        assertNotNull(result)
        assertEquals("https://example.com/main.jpg", result!!.imageUrl) // Should prefer imageUrl
    }
} 