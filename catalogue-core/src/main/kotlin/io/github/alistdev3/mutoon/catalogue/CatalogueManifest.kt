package io.github.alistdev3.mutoon.catalogue

import kotlinx.serialization.Serializable

const val SUPPORTED_CATALOGUE_SCHEMA_VERSION = 1

@Serializable
data class CatalogueManifest(
    val schemaVersion: Int,
    val books: List<CatalogueBook>,
)

@Serializable
data class CatalogueBook(
    val id: String,
    val titleAr: String,
    val titleEn: String,
    val pdfAsset: String,
    val coverAsset: String,
    val sha256: String,
    val sortOrder: Int,
)
