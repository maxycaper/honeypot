# Honeypot Barcode App

A modern Android application for organizing and managing barcodes in a hierarchical gallery
structure.

## Features

- Create and organize barcodes in hierarchical galleries
- Scan barcodes using the device camera
- Add barcodes manually
- Generate smart labels using the Google Gemini API
- Display barcode details with zoom and rotation options
- Swipe-to-delete for galleries and barcodes
- Dark theme UI with modern design

## Getting Started

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 11 or newer
- Android SDK 29 (Android 10) or newer
- Google Play Services

### Building the App

1. Clone the repository:
   ```
   git clone https://github.com/yourusername/honeypot.git
   ```

2. Open the project in Android Studio.

3. Set up the Gemini API key:
    - Create a file called `local.properties` in the project root if it doesn't exist
    - Add the following line, replacing `YOUR_API_KEY` with your actual Gemini API key:
      ```
      GEMINI_API_KEY=YOUR_API_KEY
      ```

4. Build the project:
   ```
   ./gradlew build
   ```

5. Run the app on a device or emulator:
   ```
   ./gradlew installDebug
   ```

## Testing

The project includes both automated tests and a manual testing plan.

### Running Automated Tests

#### Unit Tests

Run unit tests with:

```
./gradlew test
```

These tests cover:

- Data model validation
- ViewModel logic
- API service functionality

#### Instrumented Tests

Run instrumented tests with:

```
./gradlew connectedAndroidTest
```

These tests cover:

- UI components and interactions
- Activity and Fragment lifecycle
- Integration with device features

### Manual Testing

A comprehensive manual testing plan is available in the `docs/MANUAL_TEST_PLAN.md` file. The plan
covers:

1. Installation and First Launch
2. Gallery Management
3. Barcode Management
4. Edge Cases and Error Handling
5. Device Compatibility
6. Performance and Resource Usage

## Project Structure

```
app/
├── src/
│   ├── main/
│   │   ├── java/com/bar/honeypot/
│   │   │   ├── api/           # Gemini API integration
│   │   │   ├── barcode/       # Barcode scanning and processing
│   │   │   ├── model/         # Data models
│   │   │   ├── ui/
│   │   │   │   └── gallery/   # Gallery UI components
│   │   │   └── util/          # Utility classes
│   │   └── res/               # Resources
│   ├── test/                  # Unit tests
│   └── androidTest/           # Instrumented tests
├── build.gradle.kts           # App build configuration
└── proguard-rules.pro         # ProGuard rules
```

## 16KB Page Size Support

This app includes support for 16KB page sizes, which is required for optimal performance on newer
Android devices with Arm64 architecture. This is configured in the app's `build.gradle.kts` file:

```kotlin
packaging {
    jniLibs {
        useLegacyPackaging = false
    }
}
```

## License

[Include license information here]

## Acknowledgments

- Google Gemini API for smart label generation
- ML Kit for barcode scanning
- ZXing for barcode generation
- Material Design Components for UI elements
