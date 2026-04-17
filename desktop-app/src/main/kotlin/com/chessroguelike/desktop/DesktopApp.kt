package com.chessroguelike.desktop

import com.chessroguelike.contentio.ClasspathPackSource
import com.chessroguelike.contentio.DefaultModResolver
import com.chessroguelike.contentio.FileSystemPackSource
import java.io.File

fun main() {
    val modsDir = File(System.getProperty("user.home"), ".chess-roguelike/mods")
    val resolved = DefaultModResolver(
        baseSources = listOf(ClasspathPackSource("base-game", 0)),
        userSources = listOf(FileSystemPackSource(modsDir, 200))
    ).resolve(modsDir.listFiles()?.map { it.name }?.toSet() ?: emptySet())

    val localizer = resolved.localizer("en")
    println(localizer.resolve("ui.main.title"))
    println(localizer.resolve("ui.main.subtitle"))
    println("Loaded content hash: ${resolved.contentHash}")
    println("Supported locales: ${resolved.supportedLocales.sorted().joinToString()}")
    println("User mod directory: ${modsDir.absolutePath}")
}
