package com.chessroguelike.engine

data class Move(
    val fromRow: Int,
    val fromCol: Int,
    val toRow: Int,
    val toCol: Int,
    val capturedPiece: ChessPiece? = null
)
