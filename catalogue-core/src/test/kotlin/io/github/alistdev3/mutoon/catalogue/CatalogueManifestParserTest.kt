package io.github.alistdev3.mutoon.catalogue

import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogueManifestParserTest {
    @Test
    fun parsesVersionedManifest() {
        val manifest = CatalogueManifestParser.parse(
            """
            {
              "schemaVersion": 1,
              "books": [{
                "id": "synthetic-book",
                "titleAr": "عنوان اصطناعي",
                "titleEn": "Synthetic title",
                "pdfAsset": "catalogue/books/synthetic-book.pdf",
                "coverAsset": "catalogue/covers/synthetic-book.png",
                "sha256": "${"a".repeat(64)}",
                "sortOrder": 0
              }]
            }
            """.trimIndent(),
        )

        assertEquals(1, manifest.schemaVersion)
        assertEquals("synthetic-book", manifest.books.single().id)
        assertEquals("عنوان اصطناعي", manifest.books.single().titleAr)
    }

    @Test
    fun reportsMissingRequiredFieldWithItsName() {
        val error = runCatching {
            CatalogueManifestParser.parse(
                """
                {
                  "schemaVersion": 1,
                  "books": [{
                    "id": "synthetic-book",
                    "titleAr": "عنوان",
                    "pdfAsset": "catalogue/books/synthetic-book.pdf",
                    "coverAsset": "catalogue/covers/synthetic-book.png",
                    "sha256": "${"a".repeat(64)}",
                    "sortOrder": 0
                  }]
                }
                """.trimIndent(),
            )
        }.exceptionOrNull()

        assertTrue(error is CatalogueParseException)
        assertTrue(error?.message.orEmpty().contains("titleEn"))
    }

    @Test
    fun rejectsUnknownFields() {
        val error = runCatching {
            CatalogueManifestParser.parse("""{"schemaVersion":1,"books":[],"unexpected":true}""")
        }.exceptionOrNull()

        assertTrue(error is CatalogueParseException)
        assertTrue(error?.message.orEmpty().contains("unexpected"))
    }

    @Test
    fun checkedInManifestAndSchemaShareTheV1Contract() {
        val repositoryRoot = Path.of(checkNotNull(System.getProperty("mutoon.repositoryRoot")))
        val manifestPath = repositoryRoot.resolve("app/src/main/assets/catalogue/manifest.json")
        val schemaPath = repositoryRoot.resolve("docs/catalogue/schema-v1.json")

        val manifest = CatalogueManifestParser.parse(manifestPath)
        val schema = Json.parseToJsonElement(Files.readString(schemaPath)).jsonObject
        val requiredBookFields = schema.getValue("${'$'}defs").jsonObject
            .getValue("book").jsonObject
            .getValue("required").jsonArray
            .map { it.jsonPrimitive.content }

        assertEquals(SUPPORTED_CATALOGUE_SCHEMA_VERSION, manifest.schemaVersion)
        assertEquals(
            listOf("id", "titleAr", "titleEn", "pdfAsset", "coverAsset", "sha256", "sortOrder"),
            requiredBookFields,
        )
    }
}
