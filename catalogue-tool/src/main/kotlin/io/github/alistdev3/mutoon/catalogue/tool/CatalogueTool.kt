package io.github.alistdev3.mutoon.catalogue.tool

import io.github.alistdev3.mutoon.catalogue.CatalogueManifestParser
import io.github.alistdev3.mutoon.catalogue.CatalogueValidator
import io.github.alistdev3.mutoon.catalogue.Sha256
import java.io.PrintStream
import java.nio.file.Files
import java.nio.file.Path
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val exitCode = try {
        CatalogueCommands.run(args, System.out, System.err)
    } catch (error: Exception) {
        System.err.println("Catalogue error: ${safeMessage(error)}")
        1
    }
    if (exitCode != 0) exitProcess(exitCode)
}

object CatalogueCommands {
    fun run(args: Array<String>, out: PrintStream, err: PrintStream): Int {
        if (args.isEmpty()) {
            err.println(usage())
            return 2
        }
        val command = args.first()
        val options = parseOptions(args.drop(1))
        return when (command) {
            "validate" -> validate(
                manifestPath = requiredPath(options, "--manifest"),
                assetRoot = requiredPath(options, "--assets"),
                out = out,
                err = err,
            )
            "cover" -> generateCover(
                pdfPath = requiredPath(options, "--pdf"),
                outputPath = requiredPath(options, "--output"),
                out = out,
            )
            "checksum" -> checksum(requiredPath(options, "--file"), out)
            else -> {
                err.println("Unknown command '$command'.\n${usage()}")
                2
            }
        }
    }

    private fun validate(
        manifestPath: Path,
        assetRoot: Path,
        out: PrintStream,
        err: PrintStream,
    ): Int {
        val manifest = CatalogueManifestParser.parse(manifestPath)
        val issues = CatalogueValidator(assetRoot, PdfBoxPdfReadabilityChecker).validate(manifest)
        if (issues.isNotEmpty()) {
            err.println("Catalogue validation failed with ${issues.size} issue(s):")
            issues.forEach { err.println("  $it") }
            return 1
        }
        out.println(
            "Catalogue valid: schemaVersion=${manifest.schemaVersion}, books=${manifest.books.size}, " +
                "manifest=$manifestPath",
        )
        return 0
    }

    private fun generateCover(pdfPath: Path, outputPath: Path, out: PrintStream): Int {
        if (!Files.isRegularFile(pdfPath) || !Files.isReadable(pdfPath)) {
            throw IllegalArgumentException("PDF is missing or unreadable: $pdfPath")
        }
        val failure = PdfBoxPdfReadabilityChecker.failureReason(pdfPath)
        if (failure != null) throw IllegalArgumentException("Unreadable PDF '$pdfPath': $failure")
        FirstPageCoverGenerator.generate(pdfPath, outputPath)
        out.println(
            "Cover generated: $outputPath (${FirstPageCoverGenerator.DPI.toInt()} DPI, full media box, RGB PNG)",
        )
        return 0
    }

    private fun checksum(path: Path, out: PrintStream): Int {
        if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
            throw IllegalArgumentException("PDF is missing or unreadable: $path")
        }
        out.println(Sha256.compute(path))
        return 0
    }

    private fun parseOptions(args: List<String>): Map<String, String> {
        if (args.size % 2 != 0) throw IllegalArgumentException("Options must be supplied as --name value pairs")
        val options = linkedMapOf<String, String>()
        args.chunked(2).forEach { (name, value) ->
            if (!name.startsWith("--")) throw IllegalArgumentException("Expected an option name, found '$name'")
            if (options.put(name, value) != null) throw IllegalArgumentException("Option '$name' was supplied more than once")
        }
        return options
    }

    private fun requiredPath(options: Map<String, String>, name: String): Path =
        options[name]?.let(Path::of) ?: throw IllegalArgumentException("Missing required option $name")

    private fun usage(): String =
        "Usage: catalogue-tool <validate|cover|checksum> [options]"
}

private fun safeMessage(error: Throwable): String =
    error.message?.lineSequence()?.firstOrNull()?.take(500) ?: error::class.simpleName ?: "unknown error"
