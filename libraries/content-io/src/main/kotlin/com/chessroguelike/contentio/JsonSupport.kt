package com.chessroguelike.contentio

import kotlinx.serialization.json.Json

object JsonSupport {
    val json = Json {
        ignoreUnknownKeys = false
        prettyPrint = true
        classDiscriminator = "type"
        allowStructuredMapKeys = true
    }
}
