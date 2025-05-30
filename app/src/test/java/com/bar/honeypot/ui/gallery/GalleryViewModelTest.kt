package com.bar.honeypot.ui.gallery

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
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
    private lateinit var textObserver: Observer<String>

    private lateinit var viewModel: GalleryViewModel

    @Before
    fun setup() {
        viewModel = GalleryViewModel()
    }

    @Test
    fun `text LiveData contains gallery text`() {
        // Observe the LiveData
        viewModel.text.observeForever(textObserver)

        // Verify that the observer received the expected value
        verify(textObserver).onChanged("This is gallery Fragment")

        // Clean up
        viewModel.text.removeObserver(textObserver)
    }
} 