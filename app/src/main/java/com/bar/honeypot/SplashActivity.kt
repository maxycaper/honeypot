package com.bar.honeypot

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.bar.honeypot.util.VersionHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SplashActivity : AppCompatActivity() {


    private val handler = Handler(Looper.getMainLooper())
    private val delay = 2000L // 2 seconds


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make the app draw under the system bars for transparent navigation bar
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContentView(R.layout.activity_splash)

        // Set the version text
        setVersionText()

        // Set up click listener for logo to navigate to main activity
        val logoContainer = findViewById<FrameLayout>(R.id.logo_container)
        logoContainer.setOnClickListener {
            navigateToMainActivity()
        }
        
        // Also allow clicking anywhere on the screen to continue
        val rootView = findViewById<View>(android.R.id.content)
        rootView.setOnClickListener {
            navigateToMainActivity()
        }


        // Auto dismiss splash after 2 seconds
        handler.postDelayed({
            navigateToMainActivity()
        }, delay)

    }

    private fun setVersionText() {
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val versionName = packageInfo.versionName ?: "Unknown"

            // Format the version as per our scheme (YY.MM.VV)
            val (year, month, version) = try {
                VersionHelper.parseVersionName(versionName)
            } catch (e: Exception) {
                Triple(0, 0, 0) // Default values if parsing fails
            }

            // Get current date for build timestamp
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val currentDate = dateFormat.format(Date())

            val formattedVersion = when {
                year > 0 -> "Version ${versionName}\n${currentDate} • Build ${version}"
                else -> "Version ${versionName}\n${currentDate}"
            }

            val versionTextView = findViewById<TextView>(R.id.splash_version_text)
            versionTextView.text = formattedVersion

        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
    }

    private fun navigateToMainActivity() {

        // Remove any pending auto-navigation callbacks
        handler.removeCallbacksAndMessages(null)

        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish() // Close the splash activity so it's not in the back stack
    }

    override fun onPause() {
        super.onPause()
        // Remove any pending callbacks to prevent leaks
        handler.removeCallbacksAndMessages(null)
    }
}
