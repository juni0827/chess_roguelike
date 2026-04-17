package com.chessroguelike.contentio

import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.util.zip.ZipInputStream

object ZipModImporter {
    fun importZip(inputStream: InputStream, modsDir: File): File {
        modsDir.mkdirs()
        val stagingParent = modsDir.absoluteFile.parentFile ?: modsDir.absoluteFile
        val stagingDir = Files.createTempDirectory(stagingParent.toPath(), "${modsDir.name}-import-").toFile()
        return try {
            val extractedRoot = extractZipTo(stagingDir, inputStream)
            val finalDir = File(modsDir, extractedRoot.name)
            extractedRoot.copyRecursively(finalDir, overwrite = true)
            finalDir.canonicalFile
        } finally {
            stagingDir.deleteRecursively()
        }
    }

    private fun extractZipTo(stagingDir: File, inputStream: InputStream): File {
        val stagingPath = stagingDir.canonicalFile.toPath()
        var topLevelDirName: String? = null
        ZipInputStream(inputStream).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val normalized = entry.name.replace("\\", "/").trimStart('/')
                if (normalized.isNotBlank()) {
                    require('/' in normalized || entry.isDirectory) {
                        "Imported zip did not contain a top-level directory"
                    }
                    val firstSegment = normalized.substringBefore('/')
                    if (topLevelDirName == null) {
                        topLevelDirName = firstSegment
                    } else {
                        require(topLevelDirName == firstSegment) {
                            "Imported zip must contain exactly one top-level directory"
                        }
                    }
                    val target = File(stagingDir, normalized).canonicalFile
                    require(target.toPath().startsWith(stagingPath)) {
                        "Blocked invalid zip entry: ${entry.name}"
                    }
                    if (entry.isDirectory) {
                        target.mkdirs()
                    } else {
                        target.parentFile?.mkdirs()
                        target.outputStream().use { output -> zip.copyTo(output) }
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        val rootDirName = requireNotNull(topLevelDirName) {
            "Imported zip did not contain a top-level directory"
        }
        val rootDir = File(stagingDir, rootDirName).canonicalFile
        require(rootDir.isDirectory) { "Imported zip did not contain a top-level directory" }
        return rootDir
    }
}
