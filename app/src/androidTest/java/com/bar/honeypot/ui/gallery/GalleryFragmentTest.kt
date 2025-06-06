package com.bar.honeypot.ui.gallery

import android.Manifest
import android.os.Bundle
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.bar.honeypot.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GalleryFragmentTest {

    @get:Rule
    val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.CAMERA
    )

    private fun createFragmentArgs(): Bundle {
        return Bundle().apply {
            putString("gallery_name", "Test Gallery")
        }
    }

    @Test
    fun testAddBarcodeButtonIsDisplayed() {
        // Launch the fragment with required arguments
        launchFragmentInContainer<GalleryFragment>(fragmentArgs = createFragmentArgs())

        // Verify that the add barcode button is displayed
        onView(withId(R.id.fab_add_barcode)).check(matches(isDisplayed()))
    }

    @Test
    fun testAddBarcodeButtonClick() {
        // Launch the fragment with required arguments
        launchFragmentInContainer<GalleryFragment>(fragmentArgs = createFragmentArgs())

        // Click on the add barcode button
        onView(withId(R.id.fab_add_barcode)).perform(click())

        // Note: This test will only verify that clicking the button doesn't crash
        // Testing the actual action choice dialog and camera intent launch would require more complex testing with intents
    }
} 