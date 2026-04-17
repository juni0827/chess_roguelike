package com.chessroguelike.engine

import kotlinx.serialization.Serializable

@Serializable
data class ChessPiece(
    val id: Int,
    val type: PieceType,
    val isPlayer: Boolean,
    var row: Int,
    var col: Int,
    var abilityId: String = "ability.none",
    var shieldActive: Boolean = false
) {
    companion object {
        fun getUnicodeChar(type: PieceType, isPlayer: Boolean): String {
            return if (isPlayer) {
                when (type) {
                    PieceType.KING -> "♔"
                    PieceType.QUEEN -> "♕"
                    PieceType.ROOK -> "♖"
                    PieceType.BISHOP -> "♗"
                    PieceType.KNIGHT -> "♘"
                    PieceType.PAWN -> "♙"
                }
            } else {
                when (type) {
                    PieceType.KING -> "♚"
                    PieceType.QUEEN -> "♛"
                    PieceType.ROOK -> "♜"
                    PieceType.BISHOP -> "♝"
                    PieceType.KNIGHT -> "♞"
                    PieceType.PAWN -> "♟"
                }
            }
        }
    }
}
