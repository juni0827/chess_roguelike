package com.chessroguelike.engine

import kotlinx.serialization.Serializable

@Serializable
data class Move(
    val fromRow: Int,
    val fromCol: Int,
    val toRow: Int,
    val toCol: Int,
    val capturedPieceId: Int? = null,
    val enPassantCaptureId: Int? = null,
    val castleRookFromCol: Int? = null,
    val castleRookToCol: Int? = null
)
