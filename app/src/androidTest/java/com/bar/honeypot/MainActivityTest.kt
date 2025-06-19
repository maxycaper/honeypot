package com.bar.honeypot

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun testMainActivityLaunches() {
        // Verify that the MainActivity launches successfully
        onView(withId(R.id.drawer_layout)).check(matches(isDisplayed()))
    }

    @Test
    fun testFabIsDisplayed() {
        // Verify that the FAB is displayed
        onView(withId(R.id.fab)).check(matches(isDisplayed()))
    }

    @Test
    fun testFabClickShowsAddBarcodeDialog() {
        // Click the FAB
        onView(withId(R.id.fab)).perform(click())

        // Verify that the dialog is displayed
        onView(withId(R.id.dialog_title)).check(matches(isDisplayed()))
    }

    @Test
    fun testNavigationDrawerIsAccessible() {
        // Open the navigation drawer
        onView(withId(R.id.drawer_layout)).perform(click())

        // Verify that the navigation view is displayed
        onView(withId(R.id.gallery_view)).check(matches(isDisplayed()))
    }
}