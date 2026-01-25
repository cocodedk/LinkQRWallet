# QR Scanner Implementation Plan

## Goal
Add a QR code scanner so users can scan a QR and optionally save the URL into the wallet.

## Recommended approach
Use CameraX + ML Kit Barcode Scanning for reliable, fast detection and minimal UI work.

## Dependencies
- CameraX (camera, lifecycle, view)
- ML Kit barcode scanning (bundled model)

## UX flow
1. New "Scan" action (FAB menu or top bar button).
2. Scanner screen opens, requests camera permission if needed.
3. On successful scan:
   - If payload is a valid URL -> show a confirm sheet with title fetch + Save.
   - If not a URL -> show a friendly error with "Try again".

## Data handling
- Reuse existing URL normalization (`UrlUtils.normalizeUrl`).
- Reuse Add flow to avoid duplicating title fetch + duplicate detection.
- Add a safety check to block unsafe URLs before saving.

## Implementation steps
1. Add dependencies and permissions:
   - Camera permission in manifest.
   - CameraX + ML Kit dependencies in `app/build.gradle.kts`.
2. Create scanner screen:
   - CameraX PreviewView in Compose (`AndroidView`).
   - ML Kit analyzer to detect QR/barcodes.
   - Throttle results to avoid duplicate saves.
3. Integrate navigation:
   - Add a `scan` route.
   - Route to Add screen on successful scan (prefill URL).
4. UX polish:
   - Torch toggle, retry button, and guidance text.
   - Error states for denied permission and invalid QR.
5. Security checks:
   - Block non-http(s) schemes and local/private addresses.
   - Show a clear error message when blocked.
6. Tests:
   - Unit test for URL handling.
   - Basic UI test for navigation to Add screen on scan result (mocked).

## Open questions
- Where should the scan action live (top bar vs FAB)? the scan actoin must live there where the + is already.
- Save immediately or confirm first? Always confirm.
- Should non-URL QR payloads be saved as notes or ignored? ignored for now
