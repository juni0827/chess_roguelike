package com.chessroguelike.save

import com.chessroguelike.contentio.JsonSupport
import com.chessroguelike.game.SaveSnapshot
import java.io.File

class JsonSaveRepository(private val file: File) {
    fun load(): SaveSnapshot? {
        if (!file.exists()) return null
        return runCatching {
            JsonSupport.json.decodeFromString<SaveSnapshot>(file.readText())
        }.getOrNull()
    }

    fun save(snapshot: SaveSnapshot) {
        file.parentFile?.mkdirs()
        file.writeText(JsonSupport.json.encodeToString(SaveSnapshot.serializer(), snapshot))
    }
}
