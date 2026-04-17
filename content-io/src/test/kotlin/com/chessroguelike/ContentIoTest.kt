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
import org.junit.Test
import java.io.File

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
        val tempRoot = createTempDir(prefix = "mod-version-test")
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
}
