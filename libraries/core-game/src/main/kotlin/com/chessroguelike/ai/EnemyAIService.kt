package com.chessroguelike.ai

import com.chessroguelike.content.ContentRegistry
import com.chessroguelike.engine.ChessBoard
import com.chessroguelike.engine.ChessPiece
import com.chessroguelike.engine.Move
import com.chessroguelike.engine.MoveGenerator
import com.chessroguelike.engine.PieceType
import com.chessroguelike.game.DeterministicRng

class EnemyAIService : AiService {

    override fun getBestMove(
        board: ChessBoard,
        contentRegistry: ContentRegistry,
        round: Int,
        rng: DeterministicRng
    ): Move? {
        val enemyPieces = board.getEnemyPieces()
        if (enemyPieces.isEmpty()) return null

        val depth = when {
            round >= 4 -> 3
            round >= 2 -> 2
            else -> 1
        }

        val allMoves = mutableListOf<Pair<Move, Int>>()
        enemyPieces.forEach { piece ->
            val moves = MoveGenerator.getValidMoves(piece, board, contentRegistry)
            moves.forEach { move ->
                val score = evaluateMove(move, piece, board)
                allMoves.add(move to score)
            }
        }

        if (allMoves.isEmpty()) return null
        if (depth >= 2) {
            return getBestMoveWithLookAhead(board, enemyPieces, contentRegistry, depth, rng)
        }

        val bestScore = allMoves.maxOf { it.second }
        val bestMoves = allMoves.filter { it.second == bestScore }.map { it.first }
        return bestMoves[rng.nextInt(bestMoves.size)]
    }

    private fun getBestMoveWithLookAhead(
        board: ChessBoard,
        enemyPieces: List<ChessPiece>,
        contentRegistry: ContentRegistry,
        depth: Int,
        rng: DeterministicRng
    ): Move? {
        var bestMove: Move? = null
        var bestScore = Int.MIN_VALUE

        enemyPieces.forEach { piece ->
            val moves = MoveGenerator.getValidMoves(piece, board, contentRegistry)
            moves.forEach { move ->
                val simulatedBoard = simulateMove(board, move)
                val score = minimax(simulatedBoard, contentRegistry, depth - 1, false, Int.MIN_VALUE, Int.MAX_VALUE)
                if (score > bestScore || (score == bestScore && rng.nextBoolean())) {
                    bestScore = score
                    bestMove = move
                }
            }
        }
        return bestMove
    }

    private fun minimax(
        board: ChessBoard,
        contentRegistry: ContentRegistry,
        depth: Int,
        isMaximizing: Boolean,
        alpha: Int,
        beta: Int
    ): Int {
        if (depth == 0 || !board.isPlayerKingAlive() || !board.isEnemyKingAlive()) {
            return evaluateBoard(board)
        }

        var alphaVar = alpha
        var betaVar = beta

        return if (isMaximizing) {
            var maxScore = Int.MIN_VALUE
            loop@ for (piece in board.getEnemyPieces()) {
                for (move in MoveGenerator.getValidMoves(piece, board, contentRegistry)) {
                    val score = minimax(simulateMove(board, move), contentRegistry, depth - 1, false, alphaVar, betaVar)
                    maxScore = maxOf(maxScore, score)
                    alphaVar = maxOf(alphaVar, score)
                    if (betaVar <= alphaVar) {
                        break@loop
                    }
                }
            }
            maxScore
        } else {
            var minScore = Int.MAX_VALUE
            loop@ for (piece in board.getPlayerPieces()) {
                for (move in MoveGenerator.getValidMoves(piece, board, contentRegistry)) {
                    val score = minimax(simulateMove(board, move), contentRegistry, depth - 1, true, alphaVar, betaVar)
                    minScore = minOf(minScore, score)
                    betaVar = minOf(betaVar, score)
                    if (betaVar <= alphaVar) {
                        break@loop
                    }
                }
            }
            minScore
        }
    }

    private fun simulateMove(board: ChessBoard, move: Move): ChessBoard {
        val newBoard = ChessBoard(board.snapshot())
        val piece = newBoard.getPiece(move.fromRow, move.fromCol) ?: return newBoard
        newBoard.executeMove(piece, move)
        return newBoard
    }

    private fun evaluateMove(move: Move, piece: ChessPiece, board: ChessBoard): Int {
        var score = 0
        val target = board.getPiece(move.toRow, move.toCol)
        if (target != null && target.isPlayer) {
            score += pieceValue(target.type) * 10
        }

        val playerKing = board.getPlayerPieces().find { it.type == PieceType.KING }
        if (playerKing != null) {
            val currentDist = kotlin.math.abs(piece.row - playerKing.row) + kotlin.math.abs(piece.col - playerKing.col)
            val newDist = kotlin.math.abs(move.toRow - playerKing.row) + kotlin.math.abs(move.toCol - playerKing.col)
            score += (currentDist - newDist) * 2
        }
        return score
    }

    private fun evaluateBoard(board: ChessBoard): Int {
        if (!board.isPlayerKingAlive()) return 10_000
        if (!board.isEnemyKingAlive()) return -10_000
        val enemyScore = board.getEnemyPieces().sumOf { pieceValue(it.type) }
        val playerScore = board.getPlayerPieces().sumOf { pieceValue(it.type) }
        return enemyScore - playerScore
    }

    private fun pieceValue(type: PieceType): Int = when (type) {
        PieceType.PAWN -> 1
        PieceType.KNIGHT -> 3
        PieceType.BISHOP -> 3
        PieceType.ROOK -> 5
        PieceType.QUEEN -> 9
        PieceType.KING -> 100
    }
}
