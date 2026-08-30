package io.github.alistdev3.mutoon.catalogue

import java.io.BufferedInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

object Sha256 {
    fun compute(path: Path): String {
        val digest = MessageDigest.getInstance("SHA-256")
        BufferedInputStream(Files.newInputStream(path)).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString(separator = "") { byte -> "%02x".format(byte) }
    }
}
