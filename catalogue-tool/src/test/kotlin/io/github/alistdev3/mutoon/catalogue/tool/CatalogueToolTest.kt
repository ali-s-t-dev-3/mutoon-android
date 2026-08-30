package io.github.alistdev3.mutoon.catalogue.tool

import io.github.alistdev3.mutoon.catalogue.Sha256
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.file.Files
import javax.imageio.ImageIO
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CatalogueToolTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun coverGenerationIsDeterministicAndUsesTheCompleteMediaBox() {
        val pdf = syntheticPdf()
        val firstCover = temporaryFolder.root.toPath().resolve("first.png")
        val secondCover = temporaryFolder.root.toPath().resolve("second.png")

        FirstPageCoverGenerator.generate(pdf, firstCover)
        FirstPageCoverGenerator.generate(pdf, secondCover)

        assertEquals(Sha256.compute(firstCover), Sha256.compute(secondCover))
        val image = ImageIO.read(firstCover.toFile())
        assertNotNull(image)
        assertEquals(400, image.width)
        assertEquals(600, image.height)
    }

    @Test
    fun checksumIsStableForTheSameInput() {
        val pdf = syntheticPdf()

        assertEquals(Sha256.compute(pdf), Sha256.compute(pdf))
    }

    @Test
    fun checksumMatchesThePublishedSha256TestVector() {
        val input = temporaryFolder.newFile("sha256-input.txt").toPath()
        Files.writeString(input, "abc")

        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            Sha256.compute(input),
        )
    }

    @Test
    fun pdfInspectorRejectsUnreadableInput() {
        val invalidPdf = temporaryFolder.newFile("invalid.pdf").toPath()
        Files.writeString(invalidPdf, "synthetic invalid PDF")

        assertNotNull(PdfBoxPdfReadabilityChecker.failureReason(invalidPdf))
    }

    @Test
    fun validateCommandAcceptsSyntheticPipelineAssets() {
        val assetRoot = temporaryFolder.newFolder("assets").toPath()
        val pdf = assetRoot.resolve("catalogue/books/synthetic.pdf")
        val cover = assetRoot.resolve("catalogue/covers/synthetic.png")
        Files.createDirectories(pdf.parent)
        Files.copy(syntheticPdf(), pdf)
        FirstPageCoverGenerator.generate(pdf, cover)
        val manifest = assetRoot.resolve("catalogue/manifest.json")
        Files.writeString(
            manifest,
            """
            {
              "schemaVersion": 1,
              "books": [{
                "id": "synthetic",
                "titleAr": "عنوان اصطناعي",
                "titleEn": "Synthetic",
                "pdfAsset": "catalogue/books/synthetic.pdf",
                "coverAsset": "catalogue/covers/synthetic.png",
                "sha256": "${Sha256.compute(pdf)}",
                "sortOrder": 0
              }]
            }
            """.trimIndent(),
        )
        val stdout = ByteArrayOutputStream()
        val stderr = ByteArrayOutputStream()

        val exitCode = CatalogueCommands.run(
            arrayOf(
                "validate",
                "--manifest",
                manifest.toString(),
                "--assets",
                assetRoot.toString(),
            ),
            PrintStream(stdout),
            PrintStream(stderr),
        )

        assertEquals(stderr.toString(), 0, exitCode)
        assertTrue(stdout.toString().contains("Catalogue valid"))
        assertNull(PdfBoxPdfReadabilityChecker.failureReason(pdf))
    }

    private fun syntheticPdf() = temporaryFolder.newFile("synthetic-${System.nanoTime()}.pdf").toPath().also { path ->
        PDDocument().use { document ->
            val page = PDPage(PDRectangle(200f, 300f))
            page.cropBox = PDRectangle(50f, 50f, 100f, 200f)
            document.addPage(page)
            PDPageContentStream(document, page).use { content ->
                content.setNonStrokingColor(210f / 255f, 30f / 255f, 30f / 255f)
                content.addRect(0f, 0f, 200f, 300f)
                content.fill()
                content.setNonStrokingColor(245f / 255f, 245f / 255f, 245f / 255f)
                content.addRect(10f, 10f, 180f, 280f)
                content.fill()
            }
            document.save(path.toFile())
        }
    }
}
