package com.chessroguelike.engine

import kotlinx.serialization.Serializable

@Serializable
enum class PieceType {
    PAWN,
    ROOK,
    KNIGHT,
    BISHOP,
    QUEEN,
    KING
}
