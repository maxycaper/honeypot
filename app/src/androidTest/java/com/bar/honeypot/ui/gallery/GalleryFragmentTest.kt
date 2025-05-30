package com.bar.honeypot.ui.gallery

import android.Manifest
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

    @Test
    fun testScannerButtonIsDisplayed() {
        // Launch the fragment
        launchFragmentInContainer<GalleryFragment>()

        // Verify that the scanner button is displayed
        onView(withId(R.id.fab_scanner)).check(matches(isDisplayed()))
    }

    @Test
    fun testScannerButtonClick() {
        // Launch the fragment
        launchFragmentInContainer<GalleryFragment>()

        // Click on the scanner button
        onView(withId(R.id.fab_scanner)).perform(click())

        // Note: This test will only verify that clicking the button doesn't crash
        // Testing the actual camera intent launch would require more complex testing with intents
    }
} 