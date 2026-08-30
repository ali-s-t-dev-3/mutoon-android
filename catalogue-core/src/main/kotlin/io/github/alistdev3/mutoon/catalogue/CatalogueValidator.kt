package io.github.alistdev3.mutoon.catalogue

import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path

fun interface PdfReadabilityChecker {
    /** Returns null when the PDF is readable, otherwise an actionable failure reason. */
    fun failureReason(path: Path): String?
}

data class CatalogueValidationIssue(
    val code: String,
    val location: String,
    val message: String,
) {
    override fun toString(): String = "[$code] $location: $message"
}

class CatalogueValidator(
    assetRoot: Path,
    private val pdfReadabilityChecker: PdfReadabilityChecker,
) {
    private val assetRoot = assetRoot.toAbsolutePath().normalize()

    fun validate(manifest: CatalogueManifest): List<CatalogueValidationIssue> {
        val issues = mutableListOf<CatalogueValidationIssue>()

        if (manifest.schemaVersion != SUPPORTED_CATALOGUE_SCHEMA_VERSION) {
            issues += issue(
                "UNSUPPORTED_SCHEMA_VERSION",
                "schemaVersion",
                "expected $SUPPORTED_CATALOGUE_SCHEMA_VERSION but found ${manifest.schemaVersion}",
            )
        }

        validateDuplicateIds(manifest, issues)
        validateSortOrders(manifest, issues)

        manifest.books.forEachIndexed { index, book ->
            val base = "books[$index]"
            if (book.id.isBlank()) {
                issues += issue("MISSING_BOOK_ID", "$base.id", "book id must not be blank")
            } else if (!BOOK_ID.matches(book.id)) {
                issues += issue(
                    "INVALID_BOOK_ID",
                    "$base.id",
                    "use a lowercase ASCII slug such as 'al-ajurrumiyyah'",
                )
            }
            if (book.titleAr.isBlank()) {
                issues += issue("MISSING_TITLE_AR", "$base.titleAr", "Arabic title must not be blank")
            }
            if (book.titleEn.isBlank()) {
                issues += issue("MISSING_TITLE_EN", "$base.titleEn", "English title must not be blank")
            }
            if (book.sortOrder < 0) {
                issues += issue("INVALID_SORT_ORDER", "$base.sortOrder", "sort order must be zero or greater")
            }

            val pdf = resolveAsset(book.pdfAsset, "$base.pdfAsset", ".pdf", issues)
            resolveAsset(book.coverAsset, "$base.coverAsset", ".png", issues)

            val checksumIsValid = SHA_256.matches(book.sha256)
            if (!checksumIsValid) {
                issues += issue(
                    "INVALID_SHA256",
                    "$base.sha256",
                    "expected exactly 64 lowercase hexadecimal characters",
                )
            }

            if (pdf != null) {
                if (checksumIsValid) {
                    val actualChecksum = runCatching { Sha256.compute(pdf) }.getOrElse { error ->
                        issues += issue(
                            "CHECKSUM_READ_FAILED",
                            "$base.pdfAsset",
                            "could not calculate SHA-256: ${safeMessage(error)}",
                        )
                        null
                    }
                    if (actualChecksum != null && actualChecksum != book.sha256) {
                        issues += issue(
                            "CHECKSUM_MISMATCH",
                            "$base.sha256",
                            "declared ${book.sha256}, but ${book.pdfAsset} is $actualChecksum",
                        )
                    }
                }

                val unreadableReason = runCatching { pdfReadabilityChecker.failureReason(pdf) }
                    .getOrElse { error -> "PDF inspection failed: ${safeMessage(error)}" }
                if (unreadableReason != null) {
                    issues += issue(
                        "UNREADABLE_PDF",
                        "$base.pdfAsset",
                        unreadableReason,
                    )
                }
            }
        }

        return issues
    }

    private fun validateDuplicateIds(
        manifest: CatalogueManifest,
        issues: MutableList<CatalogueValidationIssue>,
    ) {
        val firstIndexById = mutableMapOf<String, Int>()
        manifest.books.forEachIndexed { index, book ->
            if (book.id.isBlank()) return@forEachIndexed
            val firstIndex = firstIndexById.putIfAbsent(book.id, index)
            if (firstIndex != null) {
                issues += issue(
                    "DUPLICATE_BOOK_ID",
                    "books[$index].id",
                    "'${book.id}' is already used by books[$firstIndex].id",
                )
            }
        }
    }

    private fun validateSortOrders(
        manifest: CatalogueManifest,
        issues: MutableList<CatalogueValidationIssue>,
    ) {
        val firstIndexByOrder = mutableMapOf<Int, Int>()
        manifest.books.forEachIndexed { index, book ->
            val firstIndex = firstIndexByOrder.putIfAbsent(book.sortOrder, index)
            if (firstIndex != null) {
                issues += issue(
                    "DUPLICATE_SORT_ORDER",
                    "books[$index].sortOrder",
                    "${book.sortOrder} is already used by books[$firstIndex].sortOrder",
                )
            }
        }

        val declaredOrder = manifest.books.map(CatalogueBook::sortOrder)
        if (declaredOrder != declaredOrder.sorted()) {
            issues += issue(
                "NONDETERMINISTIC_BOOK_ORDER",
                "books",
                "entries must be listed in ascending sortOrder; found $declaredOrder",
            )
        }
    }

    private fun resolveAsset(
        rawPath: String,
        location: String,
        requiredExtension: String,
        issues: MutableList<CatalogueValidationIssue>,
    ): Path? {
        if (rawPath.isBlank()) {
            issues += issue("MISSING_ASSET_PATH", location, "asset path must not be blank")
            return null
        }
        if (!rawPath.endsWith(requiredExtension, ignoreCase = true)) {
            issues += issue(
                "INVALID_ASSET_TYPE",
                location,
                "expected a $requiredExtension asset path",
            )
        }
        if (rawPath.contains('\\') || rawPath.contains(':') || rawPath.split('/').any { it.isBlank() || it == "." || it == ".." }) {
            issues += issue(
                "UNSAFE_ASSET_PATH",
                location,
                "use a relative, forward-slash path without empty, '.' or '..' segments",
            )
            return null
        }

        val relativePath = try {
            Path.of(rawPath)
        } catch (error: InvalidPathException) {
            issues += issue("UNSAFE_ASSET_PATH", location, "invalid path: ${safeMessage(error)}")
            return null
        }
        if (relativePath.isAbsolute) {
            issues += issue("UNSAFE_ASSET_PATH", location, "absolute paths are not allowed")
            return null
        }

        val candidate = assetRoot.resolve(relativePath).normalize()
        if (!candidate.startsWith(assetRoot)) {
            issues += issue("UNSAFE_ASSET_PATH", location, "path escapes the Android assets directory")
            return null
        }
        if (!Files.isRegularFile(candidate) || !Files.isReadable(candidate)) {
            issues += issue(
                "MISSING_ASSET",
                location,
                "'$rawPath' is missing, unreadable, or not a regular file beneath $assetRoot",
            )
            return null
        }

        val realRoot = runCatching { assetRoot.toRealPath() }.getOrElse { assetRoot }
        val realCandidate = runCatching { candidate.toRealPath() }.getOrElse { error ->
            issues += issue("MISSING_ASSET", location, "cannot resolve '$rawPath': ${safeMessage(error)}")
            return null
        }
        if (!realCandidate.startsWith(realRoot)) {
            issues += issue(
                "UNSAFE_ASSET_PATH",
                location,
                "symbolic link resolves outside the Android assets directory",
            )
            return null
        }
        return realCandidate
    }

    private fun issue(code: String, location: String, message: String) =
        CatalogueValidationIssue(code, location, message)

    private fun safeMessage(error: Throwable): String =
        error.message?.lineSequence()?.firstOrNull()?.take(240) ?: error::class.simpleName ?: "unknown error"

    private companion object {
        val BOOK_ID = Regex("^[a-z0-9]+(?:-[a-z0-9]+)*$")
        val SHA_256 = Regex("^[0-9a-f]{64}$")
    }
}
