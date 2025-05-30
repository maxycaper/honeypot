package com.bar.honeypot.util

/**
 * Helper class for managing versioning in the format YY.MM.VV
 * - YY: Year (e.g., 25 for 2025)
 * - MM: Month (e.g., 05 for May)
 * - VV: Version number (starts at 00 for each new month, increments for patches)
 */
object VersionHelper {
    
    /**
     * Parse the version parts from a version name string in the format "YY.MM.VV"
     * @param versionName Version name in the format "YY.MM.VV"
     * @return Triple of (year, month, version) as integers
     */
    fun parseVersionName(versionName: String): Triple<Int, Int, Int> {
        val parts = versionName.split(".")
        if (parts.size != 3) {
            throw IllegalArgumentException("Version name must be in format YY.MM.VV")
        }
        
        return try {
            val year = parts[0].toInt()
            val month = parts[1].toInt()
            val version = parts[2].toInt()
            Triple(year, month, version)
        } catch (e: NumberFormatException) {
            throw IllegalArgumentException("All version parts must be integers", e)
        }
    }
    
    /**
     * Format the version parts into a version name string
     * @param year Year part (e.g., 25 for 2025)
     * @param month Month part (1-12)
     * @param version Version part (0-99)
     * @return Version name string in the format "YY.MM.VV"
     */
    fun formatVersionName(year: Int, month: Int, version: Int): String {
        return String.format("%02d.%02d.%02d", year, month, version)
    }
    
    /**
     * Generate a version code from the version parts
     * @param year Year part (e.g., 25 for 2025)
     * @param month Month part (1-12)
     * @param version Version part (0-99)
     * @return Version code as integer (YYMMVV)
     */
    fun generateVersionCode(year: Int, month: Int, version: Int): Int {
        return year * 10000 + month * 100 + version
    }
    
    /**
     * Increment the patch version for a bug fix
     * @param currentVersionName Current version name
     * @return New version name with incremented patch version
     */
    fun incrementPatchVersion(currentVersionName: String): String {
        val (year, month, version) = parseVersionName(currentVersionName)
        return formatVersionName(year, month, version + 1)
    }
    
    /**
     * Update to a new month's version
     * @param year Year part (e.g., 25 for 2025)
     * @param month Month part (1-12)
     * @return New version name for the start of the month (YY.MM.00)
     */
    fun newMonthVersion(year: Int, month: Int): String {
        return formatVersionName(year, month, 0)
    }
} 