package io.github.alistdev3.mutoon.catalogue.tool

import io.github.alistdev3.mutoon.catalogue.PdfReadabilityChecker
import java.awt.image.BufferedImage
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import javax.imageio.ImageIO
import org.apache.pdfbox.Loader
import org.apache.pdfbox.rendering.ImageType
import org.apache.pdfbox.rendering.PDFRenderer

object PdfBoxPdfReadabilityChecker : PdfReadabilityChecker {
    override fun failureReason(path: Path): String? = try {
        Loader.loadPDF(path.toFile()).use { document ->
            if (document.numberOfPages < 1) {
                "PDF contains no pages"
            } else {
                val mediaBox = document.getPage(0).mediaBox
                if (mediaBox.width <= 0f || mediaBox.height <= 0f) {
                    "first page has an invalid media box (${mediaBox.width} x ${mediaBox.height})"
                } else {
                    null
                }
            }
        }
    } catch (error: Exception) {
        error.message?.lineSequence()?.firstOrNull()?.take(240)
            ?: error::class.simpleName
            ?: "PDF parser rejected the file"
    }
}

object FirstPageCoverGenerator {
    const val DPI = 144f

    fun generate(pdfPath: Path, outputPath: Path) {
        System.setProperty("java.awt.headless", "true")
        val image = Loader.loadPDF(pdfPath.toFile()).use { document ->
            require(document.numberOfPages > 0) { "PDF contains no pages" }
            val firstPage = document.getPage(0)
            val originalCropBox = firstPage.cropBox
            firstPage.cropBox = firstPage.mediaBox
            try {
                PDFRenderer(document).renderImageWithDPI(0, DPI, ImageType.RGB)
            } finally {
                firstPage.cropBox = originalCropBox
            }
        }
        writePngAtomically(image, outputPath.toAbsolutePath().normalize())
    }

    private fun writePngAtomically(image: BufferedImage, outputPath: Path) {
        require(outputPath.fileName.toString().endsWith(".png", ignoreCase = true)) {
            "cover output must use the .png extension"
        }
        val parent = outputPath.parent ?: throw IllegalArgumentException("cover output needs a parent directory")
        Files.createDirectories(parent)
        val temporary = Files.createTempFile(parent, ".mutoon-cover-", ".png")
        try {
            check(ImageIO.write(image, "png", temporary.toFile())) { "no PNG image writer is available" }
            try {
                Files.move(
                    temporary,
                    outputPath,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING,
                )
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temporary, outputPath, StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            Files.deleteIfExists(temporary)
        }
    }
}
