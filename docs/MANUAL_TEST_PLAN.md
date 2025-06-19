# Honeypot Barcode App Manual Test Plan

This document outlines manual testing procedures for the Honeypot Barcode App.

## 1. Installation and First Launch

### Test 1.1: Fresh Installation

1. Uninstall any previous versions of the app
2. Install the app from APK or Play Store
3. Verify app installs without errors
4. Verify splash screen appears on first launch
5. Verify main gallery screen appears after splash screen

### Test 1.2: Permission Handling

1. Launch the app
2. Attempt to scan a barcode
3. Verify camera permission request is shown
4. Test both accepting and denying permission
5. Verify appropriate behavior in each case

## 2. Gallery Management

### Test 2.1: Creating Top-Level Galleries

1. Tap the + button on the main screen
2. Enter a gallery name
3. Tap "Create"
4. Verify the new gallery appears in the list
5. Verify the gallery is empty when opened

### Test 2.2: Creating Sub-Galleries

1. Open an existing gallery
2. Tap the + button
3. Select "Add Sub-Gallery"
4. Enter a sub-gallery name
5. Tap "Create"
6. Verify the sub-gallery appears in the list
7. Verify the sub-gallery is empty when opened

### Test 2.3: Deleting Galleries

1. Swipe left on a gallery in the list
2. Tap "Delete" on the confirmation dialog
3. Verify the gallery is removed from the list
4. Verify all sub-galleries and barcodes are also deleted

## 3. Barcode Management

### Test 3.1: Manual Barcode Entry

1. Open a gallery
2. Tap the + button
3. Select "Add Barcode"
4. Select "Add Manually"
5. Enter barcode value
6. Enter barcode name
7. Tap "Add"
8. Verify the barcode appears in the list

### Test 3.2: Barcode Scanning

1. Open a gallery
2. Tap the + button
3. Select "Add Barcode"
4. Select "Scan Barcode"
5. Position a physical barcode in the camera view
6. Verify the barcode is detected
7. Verify the detected value is displayed in the form
8. Enter a name or use the AI-generated one
9. Tap "Add"
10. Verify the barcode appears in the list

### Test 3.3: Generate Smart Label

1. Open a gallery
2. Tap the + button
3. Select "Add Barcode"
4. Select "Add Manually"
5. Enter a valid barcode value (e.g., a UPC code)
6. Tap "Generate Smart Label"
7. Verify loading indicator appears
8. Verify name field is populated with AI suggestion
9. Verify AI notes field appears with description
10. Tap "Add"
11. Verify the barcode appears in the list

### Test 3.4: Viewing Barcode Details

1. Open a gallery
2. Tap on a barcode in the list
3. Verify barcode details dialog appears
4. Verify barcode image is correctly rendered
5. Verify barcode value and type are displayed
6. Verify any AI notes are displayed
7. Test double-tapping the barcode image for enlargement
8. Test pinch-to-zoom functionality on enlarged view
9. Test rotation of barcode image with long press

## 4. Edge Cases and Error Handling

### Test 4.1: No Internet Connection

1. Put device in airplane mode
2. Attempt to generate a smart label
3. Verify appropriate error message is shown
4. Verify the app allows manual entry without internet

### Test 4.2: Invalid Barcode Values

1. Attempt to add a barcode with an empty value
2. Verify appropriate validation error is shown
3. Attempt to add a barcode with invalid format
4. Verify appropriate validation error is shown

### Test 4.3: Duplicate Barcodes

1. Add a barcode with a specific value
2. Attempt to add another barcode with the same value
3. Verify appropriate error message about duplicates

### Test 4.4: Memory Management

1. Add a large number of galleries and barcodes (50+)
2. Navigate through the app
3. Verify performance remains acceptable
4. Verify no crashes or memory errors occur

## 5. Device Compatibility

### Test 5.1: Different Screen Sizes

1. Test the app on phones with different screen sizes
2. Test the app on tablets
3. Verify all UI elements scale appropriately
4. Verify all functionality works correctly

### Test 5.2: Different Android Versions

1. Test the app on devices with Android 10
2. Test the app on devices with Android 11
3. Test the app on devices with Android 12
4. Test the app on devices with Android 13
5. Verify all functionality works correctly on each version

### Test 5.3: Different Device Manufacturers

1. Test on Samsung devices
2. Test on Google Pixel devices
3. Test on other manufacturer devices
4. Verify consistent behavior across all devices

## 6. Performance and Resource Usage

### Test 6.1: Battery Usage

1. Fully charge the device
2. Use the app extensively for 30 minutes
3. Check battery usage statistics
4. Verify the app does not consume excessive battery

### Test 6.2: Storage Usage

1. Check app storage usage before adding content
2. Add multiple galleries and barcodes with product images
3. Check app storage usage after adding content
4. Verify reasonable storage consumption

### Test 6.3: Camera Performance

1. Test barcode scanning in good lighting
2. Test barcode scanning in poor lighting
3. Test barcode scanning at different distances
4. Verify consistent and accurate detection in various conditions

## Reporting Issues

When reporting issues discovered during manual testing, please include:

1. Test case ID and name
2. Steps to reproduce
3. Expected behavior
4. Actual behavior
5. Device model and Android version
6. Screenshots or videos if applicable

## Test Execution Record

| Test ID | Test Name | Date | Tester | Result | Notes |
|---------|-----------|------|--------|--------|-------|
| 1.1 | Fresh Installation | | | | |
| 1.2 | Permission Handling | | | | |
| 2.1 | Creating Top-Level Galleries | | | | |
| 2.2 | Creating Sub-Galleries | | | | |
| 2.3 | Deleting Galleries | | | | |
| 3.1 | Manual Barcode Entry | | | | |
| 3.2 | Barcode Scanning | | | | |
| 3.3 | Generate Smart Label | | | | |
| 3.4 | Viewing Barcode Details | | | | |
| 4.1 | No Internet Connection | | | | |
| 4.2 | Invalid Barcode Values | | | | |
| 4.3 | Duplicate Barcodes | | | | |
| 4.4 | Memory Management | | | | |
| 5.1 | Different Screen Sizes | | | | |
| 5.2 | Different Android Versions | | | | |
| 5.3 | Different Device Manufacturers | | | | |
| 6.1 | Battery Usage | | | | |
| 6.2 | Storage Usage | | | | |
| 6.3 | Camera Performance | | | | |