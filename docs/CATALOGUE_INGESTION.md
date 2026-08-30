# Catalogue ingestion

The Android asset `app/src/main/assets/catalogue/manifest.json` is the single source of truth for bundled books. Its current contract is `docs/catalogue/schema-v1.json`. The checked-in manifest may be empty; no PDF should be added merely to exercise this pipeline.

## Rights gate

Confirm redistribution rights before placing a book PDF in a branch, commit, pull request, build artifact, or public release. Record the evidence in the approved private rights register, the relevant restricted Jira record, or another access-controlled legal record. Keep contracts, correspondence, personal data, and other sensitive evidence out of this repository and the shipped APK.

The external record should identify the book and edition, permitted use, territory and duration if limited, evidence date, approver, and a stable reference ID. The manifest deliberately contains no legal documents or private evidence. A reference may be recorded in the coordinating Jira ticket when that does not expose sensitive material.

## Add or update a book

1. Start only after the rights gate above is complete. Use a lowercase ASCII slug for the book ID.
2. Place the approved PDF at `app/src/main/assets/catalogue/books/<id>.pdf`. Do not use a supplied sample or private path as a fixture.
3. Calculate its lowercase SHA-256 value:

   ```text
   ./gradlew catalogueChecksum -PpdfAsset=catalogue/books/<id>.pdf
   ```

   On Windows, use `gradlew.bat` in place of `./gradlew`.

4. Add the book to `manifest.json` using every v1 field:

   ```json
   {
     "id": "example-book",
     "titleAr": "العنوان العربي",
     "titleEn": "English title",
     "pdfAsset": "catalogue/books/example-book.pdf",
     "coverAsset": "catalogue/covers/example-book.png",
     "sha256": "<64 lowercase hexadecimal characters>",
     "sortOrder": 0
   }
   ```

   IDs and `sortOrder` values must be unique. Keep entries in ascending `sortOrder`. Asset paths are relative to `app/src/main/assets`, use forward slashes, and may not contain absolute, empty, `.` or `..` segments.

5. Generate the cover:

   ```text
   ./gradlew generateCatalogueCover -PpdfAsset=catalogue/books/<id>.pdf -PcoverAsset=catalogue/covers/<id>.png
   ```

   The tool renders page 1 at a fixed 144 DPI as RGB PNG. It temporarily uses the page MediaBox so the complete page is preserved, including content outside a narrower CropBox. It does not center-crop, resize to a fixed aspect ratio, or add metadata.

6. Validate the manifest and every declared asset:

   ```text
   ./gradlew validateCatalogue
   ```

7. Run the full project checks before opening a pull request:

   ```text
   ./gradlew clean validateCatalogue assembleDebug lintDebug testDebugUnitTest test
   ```

Validation reports field locations and stable error codes for unsupported schema versions, malformed records, duplicate IDs or order values, nondeterministic ordering, unsafe or missing assets, missing localized titles, invalid or mismatched SHA-256 values, and PDFs rejected by PDFBox.

## Determinism and updates

The checksum always covers the original PDF bytes. Given the same PDF bytes and pinned toolchain, rerunning cover generation produces the same PNG bytes; tests render a generated synthetic PDF twice and compare output checksums and full-page dimensions. A changed PDF must be treated as a new input: recalculate the checksum, regenerate the cover, rerun validation, and reconfirm that the recorded redistribution evidence covers that edition.
