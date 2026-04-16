package com.chessroguelike.ai

import com.chessroguelike.engine.*

class EnemyAI(private val round: Int) {

    fun getBestMove(board: ChessBoard): Move? {
        val enemyPieces = board.getEnemyPieces()
        if (enemyPieces.isEmpty()) return null

        val depth = when {
            round >= 4 -> 3
            round >= 2 -> 2
            else -> 1
        }

        val allMoves = mutableListOf<Pair<Move, Int>>()
        enemyPieces.forEach { piece ->
            val moves = MoveGenerator.getValidMoves(piece, board)
            moves.forEach { move ->
                val score = evaluateMove(move, piece, board)
                allMoves.add(Pair(move, score))
            }
        }

        if (allMoves.isEmpty()) return null

        if (depth >= 2) {
            return getBestMoveWithLookAhead(board, enemyPieces, depth)
        }

        allMoves.sortByDescending { it.second }
        val bestScore = allMoves.first().second
        val bestMoves = allMoves.filter { it.second == bestScore }
        return bestMoves.random().first
    }

    private fun getBestMoveWithLookAhead(board: ChessBoard, enemyPieces: List<ChessPiece>, depth: Int): Move? {
        var bestMove: Move? = null
        var bestScore = Int.MIN_VALUE

        enemyPieces.forEach { piece ->
            val moves = MoveGenerator.getValidMoves(piece, board)
            moves.forEach { move ->
                val simulatedBoard = simulateMove(board, move)
                val score = minimax(simulatedBoard, depth - 1, false, Int.MIN_VALUE, Int.MAX_VALUE)
                if (score > bestScore) {
                    bestScore = score
                    bestMove = move
                }
            }
        }
        return bestMove
    }

    private fun minimax(board: ChessBoard, depth: Int, isMaximizing: Boolean, alpha: Int, beta: Int): Int {
        if (depth == 0 || !board.isPlayerKingAlive() || !board.isEnemyKingAlive()) {
            return evaluateBoard(board)
        }

        var alphaVar = alpha
        var betaVar = beta

        return if (isMaximizing) {
            var maxScore = Int.MIN_VALUE
            var shouldPrune = false
            for (piece in board.getEnemyPieces()) {
                for (move in MoveGenerator.getValidMoves(piece, board)) {
                    val sim = simulateMove(board, move)
                    val score = minimax(sim, depth - 1, false, alphaVar, betaVar)
                    maxScore = maxOf(maxScore, score)
                    alphaVar = maxOf(alphaVar, score)
                    if (betaVar <= alphaVar) {
                        shouldPrune = true
                        break
                    }
                }
                if (shouldPrune) break
            }
            maxScore
        } else {
            var minScore = Int.MAX_VALUE
            var shouldPrune = false
            for (piece in board.getPlayerPieces()) {
                for (move in MoveGenerator.getValidMoves(piece, board)) {
                    val sim = simulateMove(board, move)
                    val score = minimax(sim, depth - 1, true, alphaVar, betaVar)
                    minScore = minOf(minScore, score)
                    betaVar = minOf(betaVar, score)
                    if (betaVar <= alphaVar) {
                        shouldPrune = true
                        break
                    }
                }
                if (shouldPrune) break
            }
            minScore
        }
    }

    private fun simulateMove(board: ChessBoard, move: Move): ChessBoard {
        val newBoard = ChessBoard()
        board.getAllPieces().forEach { piece ->
            newBoard.addPiece(
                ChessPiece(piece.id, piece.type, piece.isPlayer, piece.row, piece.col, piece.ability, piece.shieldActive)
            )
        }
        val piece = newBoard.getPiece(move.fromRow, move.fromCol) ?: return newBoard
        newBoard.movePiece(piece, move.toRow, move.toCol)
        return newBoard
    }

    private fun evaluateMove(move: Move, piece: ChessPiece, board: ChessBoard): Int {
        var score = 0

        // Prioritize captures
        val target = board.getPiece(move.toRow, move.toCol)
        if (target != null && target.isPlayer) {
            score += pieceValue(target.type) * 10
        }

        // Move toward player king
        val playerKing = board.getPlayerPieces().find { it.type == PieceType.KING }
        if (playerKing != null) {
            val currentDist = Math.abs(piece.row - playerKing.row) + Math.abs(piece.col - playerKing.col)
            val newDist = Math.abs(move.toRow - playerKing.row) + Math.abs(move.toCol - playerKing.col)
            score += (currentDist - newDist) * 2
        }

        // Small randomness to vary play
        score += (-2..2).random()
        return score
    }

    private fun evaluateBoard(board: ChessBoard): Int {
        if (!board.isPlayerKingAlive()) return 10000
        if (!board.isEnemyKingAlive()) return -10000

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
