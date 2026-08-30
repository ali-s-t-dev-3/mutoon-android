plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

tasks.register("validateCatalogue") {
    group = "verification"
    description = "Validates the bundled catalogue manifest and all declared assets."
    dependsOn(":catalogue-tool:validateCatalogue")
}

tasks.register("generateCatalogueCover") {
    group = "catalogue"
    description = "Renders a declared PDF asset's complete first page as a deterministic PNG cover."
    dependsOn(":catalogue-tool:generateCatalogueCover")
}

tasks.register("catalogueChecksum") {
    group = "catalogue"
    description = "Prints the SHA-256 checksum for a PDF beneath the Android assets directory."
    dependsOn(":catalogue-tool:catalogueChecksum")
}
