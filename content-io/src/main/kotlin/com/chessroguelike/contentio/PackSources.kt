package com.chessroguelike.contentio

import com.chessroguelike.content.ContentPackManifest
import com.chessroguelike.content.LocaleCatalog
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.nio.charset.StandardCharsets

data class ContentPackBundle(
    val manifest: ContentPackManifest,
    val content: GameContentFile,
    val locales: Map<String, LocaleCatalog>,
    val rawPayload: String,
    val sourcePriority: Int
)

interface ContentPackSource {
    fun loadPacks(json: Json = JsonSupport.json): List<ContentPackBundle>
}

class ClasspathPackSource(
    private val root: String,
    private val sourcePriority: Int
) : ContentPackSource {
    fun loadPacks(): List<ContentPackBundle> = loadPacks(JsonSupport.json)

    override fun loadPacks(json: Json): List<ContentPackBundle> {
        val manifestPayload = readResource("$root/manifest.json")
        val manifest = json.decodeFromString<ContentPackManifest>(manifestPayload)
        val contentPayload = readResource("$root/content/game-content.json")
        val content = json.decodeFromString<GameContentFile>(contentPayload)
        val localePayloads = listResourceLocales(root, manifest).associateWith { locale ->
            readResource("$root/locales/$locale.json")
        }
        val locales = localePayloads.mapValues { (locale, payload) ->
            LocaleCatalog(locale, json.decodeFromString(payload))
        }
        return listOf(
            ContentPackBundle(
                manifest = manifest,
                content = content,
                locales = locales,
                rawPayload = stableRawPayload(manifestPayload, contentPayload, localePayloads),
                sourcePriority = sourcePriority
            )
        )
    }

    private fun readResource(path: String): String {
        val stream = javaClass.classLoader.getResourceAsStream(path)
            ?: error("Missing classpath resource: $path")
        return stream.use { it.readBytes().toString(StandardCharsets.UTF_8) }
    }

    private fun listResourceLocales(root: String, manifest: ContentPackManifest): List<String> {
        return manifest.supportedLocales
            .filter { javaClass.classLoader.getResource("$root/locales/$it.json") != null }
    }
}

class FileSystemPackSource(
    private val rootDir: File,
    private val sourcePriority: Int
) : ContentPackSource {
    fun loadPacks(): List<ContentPackBundle> = loadPacks(JsonSupport.json)

    override fun loadPacks(json: Json): List<ContentPackBundle> {
        if (!rootDir.exists()) return emptyList()
        return rootDir.listFiles()
            ?.filter { it.isDirectory }
            ?.sortedBy { it.name }
            ?.map { loadPack(it, json) }
            ?: emptyList()
    }

    private fun loadPack(dir: File, json: Json): ContentPackBundle {
        val manifestPayload = File(dir, "manifest.json").readPayload()
        val manifest = json.decodeFromString<ContentPackManifest>(manifestPayload)
        val contentPayload = File(File(dir, "content"), "game-content.json").readPayload()
        val content = json.decodeFromString<GameContentFile>(contentPayload)
        val localesDir = File(dir, "locales")
        val localePayloads = if (localesDir.exists()) {
            localesDir.listFiles { file -> file.extension == "json" }
                ?.sortedBy { it.name }
                ?.associate { localeFile ->
                val locale = localeFile.name.removeSuffix(".json")
                locale to localeFile.readPayload()
            } ?: emptyMap()
        } else {
            emptyMap()
        }
        val locales = localePayloads.mapValues { (locale, payload) ->
            LocaleCatalog(locale, json.decodeFromString(payload))
        }
        return ContentPackBundle(
            manifest = manifest,
            content = content,
            locales = locales,
            rawPayload = stableRawPayload(manifestPayload, contentPayload, localePayloads),
            sourcePriority = sourcePriority
        )
    }

    private fun File.readPayload(): String = inputStream().use(InputStream::readBytes).toString(StandardCharsets.UTF_8)
}

private fun stableRawPayload(
    manifestPayload: String,
    contentPayload: String,
    localePayloads: Map<String, String>
): String = buildString {
    append(manifestPayload)
    append(contentPayload)
    localePayloads.toSortedMap().forEach { (locale, payload) ->
        append(locale)
        append(payload)
    }
}
