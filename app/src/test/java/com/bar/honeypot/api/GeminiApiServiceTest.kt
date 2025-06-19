package com.bar.honeypot.api

import com.google.gson.Gson
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.HttpURLConnection

class GeminiApiServiceTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var apiService: GeminiApiService
    private val gson = Gson()

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        apiService = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/").toString())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GeminiApiService::class.java)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `test generateContent success response`() {
        // Prepare mock response
        val mockResponseJson = """
            {
                "candidates": [
                    {
                        "content": {
                            "parts": [
                                {
                                    "functionResponse": {
                                        "name": "test",
                                        "response": {
                                            "productName": "Test Product",
                                            "description": "This is a test product description."
                                        }
                                    }
                                }
                            ],
                            "role": "model"
                        },
                        "finishReason": "STOP"
                    }
                ],
                "promptFeedback": null
            }
        """.trimIndent()

        val mockResponse = MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .setBody(mockResponseJson)
        mockWebServer.enqueue(mockResponse)

        // Create test request
        val request = GeminiRequest(
            contents = listOf(
                Content(
                    role = "user",
                    parts = listOf(
                        Part(text = "Test barcode value")
                    )
                )
            ),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                responseSchema = ResponseSchema(
                    type = "OBJECT",
                    properties = Properties(
                        productName = SchemaType("STRING"),
                        description = SchemaType("STRING")
                    ),
                    propertyOrdering = listOf("productName", "description")
                )
            )
        )

        // Execute the call
        val response = apiService.generateContent("test_api_key", request).execute()

        // Verify the response
        assertNotNull(response.body())
        assertEquals(true, response.isSuccessful)

        val geminiResponse = response.body()!!
        assertNotNull(geminiResponse.candidates)
        assertEquals(1, geminiResponse.candidates?.size)

        val candidate = geminiResponse.candidates!![0]
        assertNotNull(candidate.content)

        val functionResponse = candidate.content?.parts?.get(0)?.functionResponse
        assertNotNull(functionResponse)
        assertEquals("Test Product", functionResponse?.response?.productName)
        assertEquals("This is a test product description.", functionResponse?.response?.description)
    }

    @Test
    fun `test generateContent error response`() {
        // Prepare mock error response
        val mockResponse = MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST)
            .setBody("""{"error": "Invalid request"}""")
        mockWebServer.enqueue(mockResponse)

        // Create test request
        val request = GeminiRequest(
            contents = listOf(
                Content(
                    role = "user",
                    parts = listOf(
                        Part(text = "Test barcode value")
                    )
                )
            ),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                responseSchema = ResponseSchema(
                    type = "OBJECT",
                    properties = Properties(
                        productName = SchemaType("STRING"),
                        description = SchemaType("STRING")
                    ),
                    propertyOrdering = listOf("productName", "description")
                )
            )
        )

        // Execute the call
        val response = apiService.generateContent("test_api_key", request).execute()

        // Verify the response
        assertEquals(false, response.isSuccessful)
        assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, response.code())
    }
}