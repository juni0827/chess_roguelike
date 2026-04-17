package com.chessroguelike.engine

import com.chessroguelike.content.ContentRegistry
import kotlinx.serialization.Serializable
import kotlin.math.abs

@Serializable
data class BoardSnapshot(
    val pieces: List<ChessPiece>,
    val nextId: Int,
    val enPassantTargetRow: Int? = null,
    val enPassantTargetCol: Int? = null,
    val enPassantCapturePawnId: Int? = null
)

class ChessBoard(
    snapshot: BoardSnapshot? = null
) {
    private val pieces = snapshot?.pieces?.map { it.copy() }?.toMutableList() ?: mutableListOf()
    private var nextId = snapshot?.nextId ?: 1
    private var enPassantTargetRow: Int? = snapshot?.enPassantTargetRow
    private var enPassantTargetCol: Int? = snapshot?.enPassantTargetCol
    private var enPassantCapturePawnId: Int? = snapshot?.enPassantCapturePawnId

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

    fun movePiece(piece: ChessPiece, toRow: Int, toCol: Int): ChessPiece? =
        executeMove(piece, Move(piece.row, piece.col, toRow, toCol))

    fun executeMove(piece: ChessPiece, move: Move): ChessPiece? {
        val captured = if (move.enPassantCaptureId != null) {
            getPieceById(move.enPassantCaptureId)
        } else {
            getPiece(move.toRow, move.toCol)
        }
        if (captured != null) {
            if (captured.shieldActive) {
                captured.shieldActive = false
                return null
            }
            pieces.remove(captured)
        }
        val fromRow = piece.row
        piece.row = move.toRow
        piece.col = move.toCol
        piece.hasMoved = true

        if (move.castleRookFromCol != null && move.castleRookToCol != null) {
            val rook = getPiece(move.toRow, move.castleRookFromCol)
            if (rook != null && rook.type == PieceType.ROOK && rook.isPlayer == piece.isPlayer) {
                rook.col = move.castleRookToCol
                rook.hasMoved = true
            }
        }

        if (piece.type == PieceType.PAWN && kotlin.math.abs(move.toRow - fromRow) == 2) {
            enPassantTargetRow = (move.toRow + fromRow) / 2
            enPassantTargetCol = move.toCol
            enPassantCapturePawnId = piece.id
        } else {
            enPassantTargetRow = null
            enPassantTargetCol = null
            enPassantCapturePawnId = null
        }

        if (piece.type == PieceType.PAWN && (move.toRow == 0 || move.toRow == SIZE - 1)) {
            val idx = pieces.indexOfFirst { it.id == piece.id }
            if (idx >= 0) {
                pieces[idx] = pieces[idx].copy(type = PieceType.QUEEN, hasMoved = true)
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
        enPassantTargetRow = null
        enPassantTargetCol = null
        enPassantCapturePawnId = null

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

    fun enPassantTarget(): Triple<Int, Int, Int>? {
        val row = enPassantTargetRow ?: return null
        val col = enPassantTargetCol ?: return null
        val pawnId = enPassantCapturePawnId ?: return null
        return Triple(row, col, pawnId)
    }

    fun snapshot(): BoardSnapshot = BoardSnapshot(
        pieces = getAllPieces(),
        nextId = nextId,
        enPassantTargetRow = enPassantTargetRow,
        enPassantTargetCol = enPassantTargetCol,
        enPassantCapturePawnId = enPassantCapturePawnId
    )
}
