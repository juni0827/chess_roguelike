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
    override fun loadPacks(json: Json): List<ContentPackBundle> {
        val manifest = readResource("$root/manifest.json")
        val content = readResource("$root/content/game-content.json")
        val localeFiles = listResourceLocales(root)
        val locales = localeFiles.associateWith { locale ->
            val payload = readResource("$root/locales/$locale.json")
            LocaleCatalog(locale, json.decodeFromString(payload))
        }
        return listOf(
            ContentPackBundle(
                manifest = json.decodeFromString(manifest),
                content = json.decodeFromString(content),
                locales = locales,
                rawPayload = buildString {
                    append(manifest)
                    append(content)
                    locales.toSortedMap().forEach { (locale, catalog) ->
                        append(locale)
                        append(catalog.entries)
                    }
                },
                sourcePriority = sourcePriority
            )
        )
    }

    private fun readResource(path: String): String {
        val stream = javaClass.classLoader.getResourceAsStream(path)
            ?: error("Missing classpath resource: $path")
        return stream.use { it.readBytes().toString(StandardCharsets.UTF_8) }
    }

    private fun listResourceLocales(root: String): List<String> {
        return listOf("ko-KR", "en")
            .filter { javaClass.classLoader.getResource("$root/locales/$it.json") != null }
    }
}

class FileSystemPackSource(
    private val rootDir: File,
    private val sourcePriority: Int
) : ContentPackSource {
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
        val contentPayload = File(File(dir, "content"), "game-content.json").readPayload()
        val localesDir = File(dir, "locales")
        val locales = if (localesDir.exists()) {
            localesDir.listFiles { file -> file.extension == "json" }?.associate { localeFile ->
                val locale = localeFile.name.removeSuffix(".json")
                val payload = localeFile.readPayload()
                locale to LocaleCatalog(locale, json.decodeFromString(payload))
            } ?: emptyMap()
        } else {
            emptyMap()
        }
        return ContentPackBundle(
            manifest = json.decodeFromString(manifestPayload),
            content = json.decodeFromString(contentPayload),
            locales = locales,
            rawPayload = buildString {
                append(manifestPayload)
                append(contentPayload)
                locales.toSortedMap().forEach { (locale, catalog) ->
                    append(locale)
                    append(catalog.entries)
                }
            },
            sourcePriority = sourcePriority
        )
    }

    private fun File.readPayload(): String = inputStream().use(InputStream::readBytes).toString(StandardCharsets.UTF_8)
}
