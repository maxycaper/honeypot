# Honeypot App Versioning Guide

## Version Format: YY.MM.VV

The Honeypot app follows a versioning scheme based on date and sequential patch numbers:

- **YY**: Year (e.g., 25 for 2025)
- **MM**: Month (e.g., 05 for May)
- **VV**: Version number (starts at 00 for each new month, increments for patches)

## Versioning Rules

1. **New Month**: When a new month starts, update to YY.MM.00
   - Example: When June 2025 starts, update from 25.05.01 to 25.06.00

2. **Patch Updates**: For bug fixes within the same month, increment only the last part
   - Example: 25.05.00 → 25.05.01 → 25.05.02

3. **Version Code**: Use YYMMVV format for the internal version code
   - Example: Version 25.05.01 has code 250501

## How to Update the Version

### For a Patch Fix:

1. Open `app/build.gradle.kts`
2. Increment the last part of `versionName` and update `versionCode` accordingly
   - Example: Change `versionName = "25.05.01"` to `versionName = "25.05.02"`
   - Example: Change `versionCode = 250501` to `versionCode = 250502`

### For a New Month:

1. Open `app/build.gradle.kts`
2. Update the month part of `versionName` and reset the patch version to 00
   - Example: Change `versionName = "25.05.01"` to `versionName = "25.06.00"`
   - Example: Change `versionCode = 250501` to `versionCode = 250600`

## Helper Utility

Use the `VersionHelper` class for programmatically managing versions:

```kotlin
// Increment patch version
val newVersion = VersionHelper.incrementPatchVersion("25.05.01") // Returns "25.05.02"

// Create new month version
val newMonthVersion = VersionHelper.newMonthVersion(25, 6) // Returns "25.06.00"

// Generate version code
val versionCode = VersionHelper.generateVersionCode(25, 5, 1) // Returns 250501
``` 