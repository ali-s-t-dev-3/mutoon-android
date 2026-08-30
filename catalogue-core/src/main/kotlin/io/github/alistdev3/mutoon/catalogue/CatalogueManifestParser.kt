package io.github.alistdev3.mutoon.catalogue

import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class CatalogueParseException(message: String, cause: Throwable) : IllegalArgumentException(message, cause)

object CatalogueManifestParser {
    private val json = Json {
        ignoreUnknownKeys = false
        isLenient = false
    }

    fun parse(source: String): CatalogueManifest = try {
        json.decodeFromString<CatalogueManifest>(source)
    } catch (error: SerializationException) {
        throw CatalogueParseException(
            "Invalid catalogue manifest JSON: ${error.message ?: "unknown decoding error"}",
            error,
        )
    } catch (error: IllegalArgumentException) {
        throw CatalogueParseException(
            "Invalid catalogue manifest JSON: ${error.message ?: "unknown decoding error"}",
            error,
        )
    }

    fun parse(path: Path): CatalogueManifest = try {
        Files.newBufferedReader(path, Charsets.UTF_8).use { reader ->
            parse(reader.readText())
        }
    } catch (error: CatalogueParseException) {
        throw error
    } catch (error: Exception) {
        throw CatalogueParseException(
            "Cannot read catalogue manifest '$path': ${error.message ?: error::class.simpleName}",
            error,
        )
    }
}
