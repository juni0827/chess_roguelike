package com.chessroguelike.engine

import com.chessroguelike.content.ContentRegistry
import kotlinx.serialization.Serializable
import kotlin.math.abs

@Serializable
data class BoardSnapshot(
    val pieces: List<ChessPiece>,
    val nextId: Int
)

class ChessBoard(
    snapshot: BoardSnapshot? = null
) {
    private val pieces = snapshot?.pieces?.map { it.copy() }?.toMutableList() ?: mutableListOf()
    private var nextId = snapshot?.nextId ?: 1

    companion object {
        const val SIZE = 8
    }

    fun getPiece(row: Int, col: Int): ChessPiece? =
        pieces.find { it.row == row && it.col == col }

    fun getPieceById(id: Int): ChessPiece? = pieces.find { it.id == id }

    fun isValidPosition(row: Int, col: Int): Boolean =
        row in 0 until SIZE && col in 0 until SIZE

    fun getPlayerPieces(): List<ChessPiece> = pieces.filter { it.isPlayer }

    fun getEnemyPieces(): List<ChessPiece> = pieces.filter { !it.isPlayer }

    fun getAllPieces(): List<ChessPiece> = pieces.map { it.copy() }

    fun movePiece(piece: ChessPiece, toRow: Int, toCol: Int): ChessPiece? {
        val captured = getPiece(toRow, toCol)
        if (captured != null) {
            if (captured.shieldActive) {
                captured.shieldActive = false
                return null
            }
            pieces.remove(captured)
        }
        piece.row = toRow
        piece.col = toCol

        if (piece.type == PieceType.PAWN && (toRow == 0 || toRow == SIZE - 1)) {
            val idx = pieces.indexOfFirst { it.id == piece.id }
            if (idx >= 0) {
                pieces[idx] = pieces[idx].copy(type = PieceType.QUEEN)
            }
        }
        return captured
    }

    fun removePiece(piece: ChessPiece) {
        pieces.removeIf { it.id == piece.id }
    }

    fun addPiece(piece: ChessPiece) {
        pieces.add(piece)
        nextId = maxOf(nextId, piece.id + 1)
    }

    fun createAndAddPiece(
        type: PieceType,
        isPlayer: Boolean,
        row: Int,
        col: Int,
        abilityId: String = ContentRegistry.BUILTIN_ABILITY_NONE
    ): ChessPiece {
        val piece = ChessPiece(nextId++, type, isPlayer, row, col, abilityId)
        pieces.add(piece)
        return piece
    }

    fun isPlayerKingAlive(): Boolean = pieces.any { it.isPlayer && it.type == PieceType.KING }

    fun isEnemyKingAlive(): Boolean = pieces.any { !it.isPlayer && it.type == PieceType.KING }

    fun setupInitialBoard() {
        pieces.clear()
        nextId = 1

        val backRowTypes = listOf(
            PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP, PieceType.QUEEN,
            PieceType.KING, PieceType.BISHOP, PieceType.KNIGHT, PieceType.ROOK
        )
        backRowTypes.forEachIndexed { col, type ->
            createAndAddPiece(type, false, 0, col)
        }
        repeat(SIZE) { col ->
            createAndAddPiece(PieceType.PAWN, false, 1, col)
        }
        repeat(SIZE) { col ->
            createAndAddPiece(PieceType.PAWN, true, 6, col)
        }
        backRowTypes.forEachIndexed { col, type ->
            createAndAddPiece(type, true, 7, col)
        }
    }

    fun explodeAround(row: Int, col: Int, isPlayerAttacker: Boolean) {
        val toRemove = pieces.filter { piece ->
            piece.isPlayer != isPlayerAttacker &&
                abs(piece.row - row) <= 1 &&
                abs(piece.col - col) <= 1 &&
                !(piece.row == row && piece.col == col)
        }
        toRemove.forEach { piece ->
            if (piece.shieldActive) {
                piece.shieldActive = false
            } else {
                removePiece(piece)
            }
        }
    }

    fun snapshot(): BoardSnapshot = BoardSnapshot(getAllPieces(), nextId)
}
