package com.chessroguelike.engine

import com.chessroguelike.content.AbilityEffectType
import com.chessroguelike.content.ContentRegistry

object MoveGenerator {

    fun getValidMoves(piece: ChessPiece, board: ChessBoard, contentRegistry: ContentRegistry): List<Move> {
        val baseMoves = when (piece.type) {
            PieceType.PAWN -> getPawnMoves(piece, board)
            PieceType.ROOK -> getRookMoves(piece, board)
            PieceType.KNIGHT -> getKnightMoves(piece, board)
            PieceType.BISHOP -> getBishopMoves(piece, board)
            PieceType.QUEEN -> getQueenMoves(piece, board)
            PieceType.KING -> getKingMoves(piece, board)
        }

        return if (contentRegistry.abilityDefinition(piece.abilityId).effectType == AbilityEffectType.EXTRA_RANGE) {
            applyExtraRange(piece, board, baseMoves)
        } else {
            baseMoves
        }
    }

    private fun applyExtraRange(piece: ChessPiece, board: ChessBoard, baseMoves: List<Move>): List<Move> {
        val extra = mutableListOf<Move>()
        when (piece.type) {
            PieceType.ROOK -> {
                extra.addAll(getSlidingMoves(piece, board, listOf(Pair(-1, 0), Pair(1, 0), Pair(0, -1), Pair(0, 1)), 2))
            }
            PieceType.BISHOP -> {
                extra.addAll(getSlidingMoves(piece, board, listOf(Pair(-1, -1), Pair(-1, 1), Pair(1, -1), Pair(1, 1)), 2))
            }
            PieceType.KNIGHT -> {
                val extendedOffsets = listOf(
                    Pair(-3, -1), Pair(-3, 1), Pair(3, -1), Pair(3, 1),
                    Pair(-1, -3), Pair(-1, 3), Pair(1, -3), Pair(1, 3)
                )
                extendedOffsets.forEach { (dr, dc) ->
                    val nr = piece.row + dr
                    val nc = piece.col + dc
                    if (board.isValidPosition(nr, nc)) {
                        val target = board.getPiece(nr, nc)
                        if (target == null || target.isPlayer != piece.isPlayer) {
                            extra.add(Move(piece.row, piece.col, nr, nc, target?.id))
                        }
                    }
                }
            }
            else -> Unit
        }
        return (baseMoves + extra).distinctBy { Pair(it.toRow, it.toCol) }
    }

    private fun getSlidingMoves(
        piece: ChessPiece,
        board: ChessBoard,
        directions: List<Pair<Int, Int>>,
        maxSteps: Int = ChessBoard.SIZE
    ): List<Move> {
        val moves = mutableListOf<Move>()
        directions.forEach { (dr, dc) ->
            var steps = 0
            var r = piece.row + dr
            var c = piece.col + dc
            while (board.isValidPosition(r, c) && steps < maxSteps) {
                val target = board.getPiece(r, c)
                if (target == null) {
                    moves.add(Move(piece.row, piece.col, r, c))
                } else {
                    if (target.isPlayer != piece.isPlayer) {
                        moves.add(Move(piece.row, piece.col, r, c, target.id))
                    }
                    break
                }
                r += dr
                c += dc
                steps++
            }
        }
        return moves
    }

    private fun getPawnMoves(piece: ChessPiece, board: ChessBoard): List<Move> {
        val moves = mutableListOf<Move>()
        val direction = if (piece.isPlayer) -1 else 1
        val startRow = if (piece.isPlayer) 6 else 1

        val nr = piece.row + direction
        if (board.isValidPosition(nr, piece.col) && board.getPiece(nr, piece.col) == null) {
            moves.add(Move(piece.row, piece.col, nr, piece.col))
            if (piece.row == startRow) {
                val nr2 = piece.row + direction * 2
                if (board.isValidPosition(nr2, piece.col) && board.getPiece(nr2, piece.col) == null) {
                    moves.add(Move(piece.row, piece.col, nr2, piece.col))
                }
            }
        }

        listOf(-1, 1).forEach { dc ->
            val nc = piece.col + dc
            if (board.isValidPosition(nr, nc)) {
                val target = board.getPiece(nr, nc)
                if (target != null && target.isPlayer != piece.isPlayer) {
                    moves.add(Move(piece.row, piece.col, nr, nc, target.id))
                }
            }
        }
        return moves
    }

    private fun getRookMoves(piece: ChessPiece, board: ChessBoard): List<Move> =
        getSlidingMoves(piece, board, listOf(Pair(-1, 0), Pair(1, 0), Pair(0, -1), Pair(0, 1)))

    private fun getKnightMoves(piece: ChessPiece, board: ChessBoard): List<Move> {
        val moves = mutableListOf<Move>()
        val offsets = listOf(
            Pair(-2, -1), Pair(-2, 1), Pair(-1, -2), Pair(-1, 2),
            Pair(1, -2), Pair(1, 2), Pair(2, -1), Pair(2, 1)
        )
        offsets.forEach { (dr, dc) ->
            val nr = piece.row + dr
            val nc = piece.col + dc
            if (board.isValidPosition(nr, nc)) {
                val target = board.getPiece(nr, nc)
                if (target == null || target.isPlayer != piece.isPlayer) {
                    moves.add(Move(piece.row, piece.col, nr, nc, target?.id))
                }
            }
        }
        return moves
    }

    private fun getBishopMoves(piece: ChessPiece, board: ChessBoard): List<Move> =
        getSlidingMoves(piece, board, listOf(Pair(-1, -1), Pair(-1, 1), Pair(1, -1), Pair(1, 1)))

    private fun getQueenMoves(piece: ChessPiece, board: ChessBoard): List<Move> =
        getSlidingMoves(piece, board, listOf(
            Pair(-1, 0), Pair(1, 0), Pair(0, -1), Pair(0, 1),
            Pair(-1, -1), Pair(-1, 1), Pair(1, -1), Pair(1, 1)
        ))

    private fun getKingMoves(piece: ChessPiece, board: ChessBoard): List<Move> {
        val moves = mutableListOf<Move>()
        for (dr in -1..1) {
            for (dc in -1..1) {
                if (dr == 0 && dc == 0) continue
                val nr = piece.row + dr
                val nc = piece.col + dc
                if (board.isValidPosition(nr, nc)) {
                    val target = board.getPiece(nr, nc)
                    if (target == null || target.isPlayer != piece.isPlayer) {
                        moves.add(Move(piece.row, piece.col, nr, nc, target?.id))
                    }
                }
            }
        }
        return moves
    }
}
