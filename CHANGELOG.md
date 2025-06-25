# Changelog

All notable changes to the Honeypot app will be documented in this file.

## [25.06.20-bugfix3] - 2025-06-20

### Fixed

- Fixed persistent product name storage issue by adding direct update capability
- Added functionality to update existing barcodes with product information
- Ensured product names and images are properly saved and restored
- Fixed font loading errors by switching to system fonts

## [25.06.20-bugfix2] - 2025-06-20

### Fixed
- Added fallback mechanism for product name generation when API lookup fails
- Improved product name handling in barcode scanner to ensure names are never lost
- Enhanced product name categorization based on barcode format and prefix

## [25.06.20-bugfix] - 2025-06-20

### Fixed
- Fixed a bug where product names weren't being displayed correctly for scanned barcodes
- Ensured product information is properly preserved throughout the app's data flow
- Improved handling of product titles in the barcode list view

## [25.06.19] - 2025-06-19

### Added
- Initial app release with barcode scanning functionality
- Support for product information lookup
- Gallery feature for saved barcodes
- Barcode detail view with metadata display
- Support for taking product photos

