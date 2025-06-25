package com.bar.honeypot.barcode

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bar.honeypot.R
import org.hamcrest.CoreMatchers.not
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AddBarcodeDialogFragmentTest {

    @Test
    fun testInitialUiState() {
        // Launch fragment
        launchFragmentInContainer<AddBarcodeDialogFragment>()

        // Check initial UI state
        onView(withId(R.id.methodChoiceLayout)).check(matches(isDisplayed()))
        onView(withId(R.id.scanBarcodeButton)).check(matches(isDisplayed()))
        onView(withId(R.id.addManuallyButton)).check(matches(isDisplayed()))
        onView(withId(R.id.barcodeInputLayout)).check(matches(not(isDisplayed())))
    }

    @Test
    fun testScanBarcodeButton() {
        // Launch fragment
        launchFragmentInContainer<AddBarcodeDialogFragment>()

        // Click scan barcode button
        onView(withId(R.id.scanBarcodeButton)).perform(click())

        // Check UI state after clicking scan button
        onView(withId(R.id.methodChoiceLayout)).check(matches(not(isDisplayed())))
        onView(withId(R.id.scanningLayout)).check(matches(isDisplayed()))
    }

    @Test
    fun testAddManuallyButton() {
        // Launch fragment
        launchFragmentInContainer<AddBarcodeDialogFragment>()

        // Click add manually button
        onView(withId(R.id.addManuallyButton)).perform(click())

        // Check UI state after clicking add manually button
        onView(withId(R.id.methodChoiceLayout)).check(matches(not(isDisplayed())))
        onView(withId(R.id.barcodeInputLayout)).check(matches(isDisplayed()))
        onView(withId(R.id.barcodeValueEditText)).check(matches(isEnabled()))
        onView(withId(R.id.nameEditText)).check(matches(isDisplayed()))
        onView(withId(R.id.generateSmartLabelButton)).check(matches(isDisplayed()))
    }

    @Test
    fun testManualBarcodeAddition() {
        // Launch fragment
        launchFragmentInContainer<AddBarcodeDialogFragment>()

        // Navigate to manual input state
        onView(withId(R.id.addManuallyButton)).perform(click())

        // Enter barcode data
        onView(withId(R.id.barcodeValueEditText)).perform(
            typeText("123456789012"),
            closeSoftKeyboard()
        )
        onView(withId(R.id.nameEditText)).perform(
            typeText("Test Barcode"),
            closeSoftKeyboard()
        )

        // Check if add button is enabled
        onView(withId(R.id.addButton)).check(matches(isEnabled()))

        // Click add button
        onView(withId(R.id.addButton)).perform(click())

        // Fragment should be dismissed after adding, no further assertions needed
    }

    @Test
    fun testGenerateSmartLabelButton() {
        // Launch fragment
        launchFragmentInContainer<AddBarcodeDialogFragment>()

        // Navigate to manual input state
        onView(withId(R.id.addManuallyButton)).perform(click())

        // Enter barcode value
        onView(withId(R.id.barcodeValueEditText)).perform(
            typeText("123456789012"),
            closeSoftKeyboard()
        )

        // Check if generate smart label button is enabled
        onView(withId(R.id.generateSmartLabelButton)).check(matches(isEnabled()))

        // Note: We can't fully test the API call in an instrumented test,
        // but we can verify the button is clickable
        onView(withId(R.id.generateSmartLabelButton)).perform(click())

        // Verify the button shows loading state
        onView(withId(R.id.smartLabelProgressBar)).check(matches(isDisplayed()))
    }
}