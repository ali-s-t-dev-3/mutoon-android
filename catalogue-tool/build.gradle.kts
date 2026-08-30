import org.gradle.api.GradleException
import org.gradle.process.CommandLineArgumentProvider
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":catalogue-core"))
    implementation(libs.pdfbox)

    testImplementation(libs.junit4)
}

application {
    mainClass.set("io.github.alistdev3.mutoon.catalogue.tool.CatalogueToolKt")
}

val androidAssets = rootProject.layout.projectDirectory.dir("app/src/main/assets")
val manifestFile = androidAssets.file("catalogue/manifest.json")

fun checkedAssetPath(rawPath: String): String {
    if (
        rawPath.contains('\\') ||
        rawPath.contains(':') ||
        rawPath.split('/').any { it.isBlank() || it == "." || it == ".." }
    ) {
        throw GradleException(
            "Asset properties must be relative forward-slash paths without empty, '.' or '..' segments: $rawPath",
        )
    }
    val root = androidAssets.asFile.toPath().toAbsolutePath().normalize()
    val resolved = root.resolve(rawPath).normalize()
    if (!resolved.startsWith(root)) {
        throw GradleException("Asset property escapes the Android assets directory: $rawPath")
    }
    return resolved.toString()
}

tasks.register<JavaExec>("validateCatalogue") {
    group = "verification"
    description = "Validates the bundled catalogue manifest and declared assets."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set(application.mainClass)
    args(
        "validate",
        "--manifest",
        manifestFile.asFile.absolutePath,
        "--assets",
        androidAssets.asFile.absolutePath,
    )
}

tasks.register<JavaExec>("generateCatalogueCover") {
    group = "catalogue"
    description = "Renders an uncropped first PDF page. Requires -PpdfAsset and -PcoverAsset."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set(application.mainClass)
    val pdfAsset = providers.gradleProperty("pdfAsset")
    val coverAsset = providers.gradleProperty("coverAsset")
    doFirst {
        if (!pdfAsset.isPresent || !coverAsset.isPresent) {
            throw GradleException(
                "Supply -PpdfAsset=catalogue/books/<id>.pdf and " +
                    "-PcoverAsset=catalogue/covers/<id>.png",
            )
        }
    }
    argumentProviders.add(CommandLineArgumentProvider {
        listOf(
            "cover",
            "--pdf",
            checkedAssetPath(pdfAsset.get()),
            "--output",
            checkedAssetPath(coverAsset.get()),
        )
    })
}

tasks.register<JavaExec>("catalogueChecksum") {
    group = "catalogue"
    description = "Prints a PDF's SHA-256. Requires -PpdfAsset."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set(application.mainClass)
    val pdfAsset = providers.gradleProperty("pdfAsset")
    doFirst {
        if (!pdfAsset.isPresent) {
            throw GradleException("Supply -PpdfAsset=catalogue/books/<id>.pdf")
        }
    }
    argumentProviders.add(CommandLineArgumentProvider {
        listOf(
            "checksum",
            "--file",
            checkedAssetPath(pdfAsset.get()),
        )
    })
}

tasks.test {
    useJUnit()
}
