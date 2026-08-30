package io.github.alistdev3.mutoon.catalogue

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CatalogueValidatorTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun acceptsValidManifestAndMatchingAssets() {
        val root = temporaryFolder.root.toPath()
        val pdf = writeAsset(root, "catalogue/books/valid.pdf", "synthetic PDF bytes")
        writeAsset(root, "catalogue/covers/valid.png", "synthetic PNG bytes")
        val manifest = manifest(book(sha256 = Sha256.compute(pdf)))

        val issues = validator(root).validate(manifest)

        assertTrue(issues.toString(), issues.isEmpty())
    }

    @Test
    fun reportsUnsupportedSchemaVersion() {
        val root = temporaryFolder.root.toPath()
        val issues = validator(root).validate(
            CatalogueManifest(schemaVersion = 2, books = emptyList()),
        )

        assertTrue(issues.any { it.code == "UNSUPPORTED_SCHEMA_VERSION" })
    }

    @Test
    fun reportsDuplicateIdsAndSortOrders() {
        val root = temporaryFolder.root.toPath()
        val pdf = writeAsset(root, "catalogue/books/valid.pdf", "synthetic PDF bytes")
        writeAsset(root, "catalogue/covers/valid.png", "synthetic PNG bytes")
        val checksum = Sha256.compute(pdf)
        val manifest = manifest(
            book(sha256 = checksum),
            book(sha256 = checksum),
        )

        val codes = validator(root).validate(manifest).map(CatalogueValidationIssue::code)

        assertTrue(codes.contains("DUPLICATE_BOOK_ID"))
        assertTrue(codes.contains("DUPLICATE_SORT_ORDER"))
    }

    @Test
    fun reportsUnsafeAndMissingPathsBlankTitlesAndInvalidChecksum() {
        val root = temporaryFolder.root.toPath()
        val manifest = manifest(
            book(
                titleAr = "  ",
                titleEn = "",
                pdfAsset = "../outside.pdf",
                coverAsset = "catalogue/covers/missing.png",
                sha256 = "not-a-checksum",
            ),
        )

        val codes = validator(root).validate(manifest).map(CatalogueValidationIssue::code)

        assertTrue(codes.contains("MISSING_TITLE_AR"))
        assertTrue(codes.contains("MISSING_TITLE_EN"))
        assertTrue(codes.contains("UNSAFE_ASSET_PATH"))
        assertTrue(codes.contains("MISSING_ASSET"))
        assertTrue(codes.contains("INVALID_SHA256"))
    }

    @Test
    fun reportsChecksumMismatchAndUnreadablePdf() {
        val root = temporaryFolder.root.toPath()
        writeAsset(root, "catalogue/books/valid.pdf", "not actually a PDF")
        writeAsset(root, "catalogue/covers/valid.png", "synthetic PNG bytes")
        val checker = PdfReadabilityChecker { "synthetic PDF parser failure" }

        val issues = CatalogueValidator(root, checker).validate(manifest(book()))
        val codes = issues.map(CatalogueValidationIssue::code)

        assertTrue(codes.contains("CHECKSUM_MISMATCH"))
        assertTrue(codes.contains("UNREADABLE_PDF"))
        assertTrue(issues.any { it.message.contains("synthetic PDF parser failure") })
    }

    @Test
    fun reportsEntriesThatAreNotListedInSortOrder() {
        val root = temporaryFolder.root.toPath()
        val firstPdf = writeAsset(root, "catalogue/books/first.pdf", "first")
        val secondPdf = writeAsset(root, "catalogue/books/second.pdf", "second")
        writeAsset(root, "catalogue/covers/first.png", "first cover")
        writeAsset(root, "catalogue/covers/second.png", "second cover")
        val manifest = manifest(
            book(
                id = "first",
                pdfAsset = "catalogue/books/first.pdf",
                coverAsset = "catalogue/covers/first.png",
                sha256 = Sha256.compute(firstPdf),
                sortOrder = 2,
            ),
            book(
                id = "second",
                pdfAsset = "catalogue/books/second.pdf",
                coverAsset = "catalogue/covers/second.png",
                sha256 = Sha256.compute(secondPdf),
                sortOrder = 1,
            ),
        )

        val codes = validator(root).validate(manifest).map(CatalogueValidationIssue::code)

        assertTrue(codes.contains("NONDETERMINISTIC_BOOK_ORDER"))
    }

    private fun validator(root: Path) = CatalogueValidator(root, PdfReadabilityChecker { null })

    private fun writeAsset(root: Path, relativePath: String, contents: String): Path {
        val path = root.resolve(relativePath)
        Files.createDirectories(path.parent)
        Files.writeString(path, contents)
        return path
    }

    private fun manifest(vararg books: CatalogueBook) = CatalogueManifest(
        schemaVersion = SUPPORTED_CATALOGUE_SCHEMA_VERSION,
        books = books.toList(),
    )

    private fun book(
        id: String = "valid",
        titleAr: String = "عنوان",
        titleEn: String = "Title",
        pdfAsset: String = "catalogue/books/valid.pdf",
        coverAsset: String = "catalogue/covers/valid.png",
        sha256: String = "0".repeat(64),
        sortOrder: Int = 0,
    ) = CatalogueBook(
        id = id,
        titleAr = titleAr,
        titleEn = titleEn,
        pdfAsset = pdfAsset,
        coverAsset = coverAsset,
        sha256 = sha256,
        sortOrder = sortOrder,
    )
}
