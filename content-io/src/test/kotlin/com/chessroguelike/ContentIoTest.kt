package com.chessroguelike

import com.chessroguelike.content.ContentPackManifest
import com.chessroguelike.content.LocaleCatalog
import com.chessroguelike.content.VersionRange
import com.chessroguelike.contentio.ClasspathPackSource
import com.chessroguelike.contentio.ContentPackBundle
import com.chessroguelike.contentio.DefaultModResolver
import com.chessroguelike.contentio.FileSystemPackSource
import com.chessroguelike.contentio.GameContentFile
import com.chessroguelike.contentio.JsonSupport
import com.chessroguelike.contentio.ResolvedContentRegistry
import com.chessroguelike.engine.PieceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ContentIoTest {

    @Test
    fun `classpath pack source loads base content`() {
        val packs = ClasspathPackSource("base-game", 0).loadPacks()

        assertEquals(1, packs.size)
        assertTrue(packs.first().content.upgrades.isNotEmpty())
    }

    @Test
    fun `localizer falls back to english key`() {
        val registry = DefaultModResolver(
            baseSources = listOf(ClasspathPackSource("base-game", 0))
        ).resolve(emptySet())

        val value = registry.localizer("fr").resolve("ui.main.start")

        assertEquals("Start New Run", value)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `resolver rejects incompatible mod version`() {
        val tempRoot = Files.createTempDirectory("mod-version-test").toFile()
        val packDir = File(tempRoot, "bad-mod").apply { mkdirs() }
        File(packDir, "content").mkdirs()
        File(packDir, "locales").mkdirs()
        File(packDir, "manifest.json").writeText(
            JsonSupport.json.encodeToString(
                ContentPackManifest.serializer(),
                ContentPackManifest(
                    id = "bad-mod",
                    version = "1.0.0",
                    compatibleGameVersions = VersionRange("9.9.9"),
                    dependencies = emptyList(),
                    loadOrderHint = 0,
                    supportedLocales = listOf("en")
                )
            )
        )
        File(packDir, "content/game-content.json").writeText(
            JsonSupport.json.encodeToString(GameContentFile.serializer(), ClasspathPackSource("base-game", 0).loadPacks().first().content)
        )
        File(packDir, "locales/en.json").writeText(
            """{"ui.main.start":"Broken"}"""
        )

        DefaultModResolver(
            baseSources = listOf(ClasspathPackSource("base-game", 0)),
            userSources = listOf(FileSystemPackSource(tempRoot, 200))
        ).resolve(setOf("bad-mod"))
    }

    @Test
    fun `later packs override earlier definitions`() {
        val baseContent = ClasspathPackSource("base-game", 0).loadPacks().first()
        val overrideContent = baseContent.content.copy(
            rounds = baseContent.content.rounds,
            pieces = baseContent.content.pieces.map {
                if (it.pieceType == PieceType.QUEEN) it.copy(nameKey = com.chessroguelike.content.TextKey("piece.queen.override")) else it
            }
        )
        val overridePack = ContentPackBundle(
            manifest = ContentPackManifest(
                id = "override-mod",
                version = "1.0.0",
                compatibleGameVersions = VersionRange("1.0.0", "1.0.0"),
                dependencies = emptyList(),
                loadOrderHint = 10,
                supportedLocales = listOf("en")
            ),
            content = overrideContent,
            locales = mapOf("en" to LocaleCatalog("en", mapOf("piece.queen.override" to "Storm Queen"))),
            rawPayload = "override",
            sourcePriority = 200
        )

        val resolved = ResolvedContentRegistry.resolve(
            basePacks = listOf(baseContent),
            userPacks = listOf(overridePack),
            enabledModIds = setOf("override-mod")
        )

        assertEquals("Storm Queen", resolved.localizer("en").resolve(resolved.pieceNameKey(PieceType.QUEEN).value))
    }

    @Test
    fun `zip importer blocks zip slip entries`() {
        val tempRoot = Files.createTempDirectory("zip-slip-test").toFile()
        val modsDir = File(tempRoot, "mods")

        try {
            val payload = zipOf("../evil.txt" to "oops")

            try {
                com.chessroguelike.contentio.ZipModImporter.importZip(ByteArrayInputStream(payload), modsDir)
                fail("Expected zip slip import to fail")
            } catch (_: IllegalArgumentException) {
                assertTrue(File(tempRoot, "evil.txt").exists().not())
            }
        } finally {
            tempRoot.deleteRecursively()
        }
    }

    @Test
    fun `zip importer rejects zips without a top level directory`() {
        val tempRoot = Files.createTempDirectory("zip-layout-test").toFile()
        val modsDir = File(tempRoot, "mods")

        try {
            val payload = zipOf("manifest.json" to "{}")

            try {
                com.chessroguelike.contentio.ZipModImporter.importZip(ByteArrayInputStream(payload), modsDir)
                fail("Expected root-file zip to fail")
            } catch (_: IllegalArgumentException) {
                assertTrue(modsDir.listFiles().isNullOrEmpty())
            }
        } finally {
            tempRoot.deleteRecursively()
        }
    }

    @Test
    fun `zip importer rejects multiple top level directories`() {
        val tempRoot = Files.createTempDirectory("zip-multi-root-test").toFile()
        val modsDir = File(tempRoot, "mods")

        try {
            val payload = zipOf(
                "mod-a/manifest.json" to "{}",
                "mod-b/content/game-content.json" to "{}"
            )

            try {
                com.chessroguelike.contentio.ZipModImporter.importZip(ByteArrayInputStream(payload), modsDir)
                fail("Expected multi-root zip to fail")
            } catch (_: IllegalArgumentException) {
                assertTrue(modsDir.listFiles().isNullOrEmpty())
            }
        } finally {
            tempRoot.deleteRecursively()
        }
    }

    private fun zipOf(vararg entries: Pair<String, String>): ByteArray {
        val buffer = ByteArrayOutputStream()
        ZipOutputStream(buffer).use { zip ->
            entries.forEach { (name, contents) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(contents.toByteArray(StandardCharsets.UTF_8))
                zip.closeEntry()
            }
        }
        return buffer.toByteArray()
    }
}
