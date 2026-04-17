package com.chessroguelike.contentio

import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream

object ZipModImporter {
    fun importZip(inputStream: InputStream, modsDir: File): File {
        modsDir.mkdirs()
        var rootDir: File? = null
        ZipInputStream(inputStream).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val normalized = entry.name.replace("\\", "/").trimStart('/')
                if (normalized.isNotBlank()) {
                    val target = File(modsDir, normalized).canonicalFile
                    require(target.path.startsWith(modsDir.canonicalPath)) { "Blocked invalid zip entry: ${entry.name}" }
                    if (entry.isDirectory) {
                        target.mkdirs()
                    } else {
                        target.parentFile?.mkdirs()
                        target.outputStream().use { output -> zip.copyTo(output) }
                    }
                    if (rootDir == null) {
                        val firstSegment = normalized.substringBefore('/')
                        rootDir = File(modsDir, firstSegment)
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return requireNotNull(rootDir) { "Imported zip did not contain a top-level directory" }
    }
}
