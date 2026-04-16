package com.chessroguelike.engine

enum class TurnState {
    PLAYER_TURN,
    ENEMY_TURN,
    PLAYER_DOUBLE_MOVE,
    ROUND_WON,
    GAME_OVER
}

class GameEngine {
    val board = ChessBoard()
    var turnState = TurnState.PLAYER_TURN
        private set
    var selectedPiece: ChessPiece? = null
        private set
    var validMoves: List<Move> = emptyList()
        private set
    var capturedByPlayer = 0
        private set
    var capturedByEnemy = 0
        private set
    private var doubleMoveUsed = false

    fun initGame() {
        board.setupInitialBoard()
        turnState = TurnState.PLAYER_TURN
        selectedPiece = null
        validMoves = emptyList()
        capturedByPlayer = 0
        capturedByEnemy = 0
        doubleMoveUsed = false
    }

    fun selectPiece(row: Int, col: Int): Boolean {
        if (turnState != TurnState.PLAYER_TURN && turnState != TurnState.PLAYER_DOUBLE_MOVE) return false
        val piece = board.getPiece(row, col)
        if (piece == null || !piece.isPlayer) {
            selectedPiece = null
            validMoves = emptyList()
            return false
        }
        selectedPiece = piece
        validMoves = MoveGenerator.getValidMoves(piece, board)
        return true
    }

    fun makePlayerMove(toRow: Int, toCol: Int): MoveResult {
        if (turnState != TurnState.PLAYER_TURN && turnState != TurnState.PLAYER_DOUBLE_MOVE) {
            return MoveResult.INVALID
        }
        val piece = selectedPiece ?: return MoveResult.INVALID
        val move = validMoves.find { it.toRow == toRow && it.toCol == toCol } ?: return MoveResult.INVALID

        val captured = board.movePiece(piece, toRow, toCol)
        if (captured != null) {
            capturedByPlayer++
            if (piece.ability == Ability.EXPLOSION) {
                board.explodeAround(toRow, toCol, true)
            }
        }

        selectedPiece = null
        validMoves = emptyList()

        if (!board.isEnemyKingAlive()) {
            turnState = TurnState.ROUND_WON
            return MoveResult.ROUND_WON
        }

        if (turnState == TurnState.PLAYER_DOUBLE_MOVE) {
            doubleMoveUsed = true
            turnState = TurnState.ENEMY_TURN
            return MoveResult.MOVE_OK
        }

        if (piece.ability == Ability.DOUBLE_MOVE && !doubleMoveUsed) {
            doubleMoveUsed = true
            turnState = TurnState.PLAYER_DOUBLE_MOVE
            selectedPiece = piece
            validMoves = MoveGenerator.getValidMoves(piece, board)
            return MoveResult.DOUBLE_MOVE_AVAILABLE
        }

        doubleMoveUsed = false
        turnState = TurnState.ENEMY_TURN
        return MoveResult.MOVE_OK
    }

    fun skipDoubleMove() {
        if (turnState == TurnState.PLAYER_DOUBLE_MOVE) {
            doubleMoveUsed = false
            selectedPiece = null
            validMoves = emptyList()
            turnState = TurnState.ENEMY_TURN
        }
    }

    fun makeEnemyMove(move: Move): MoveResult {
        if (turnState != TurnState.ENEMY_TURN) return MoveResult.INVALID
        val piece = board.getPiece(move.fromRow, move.fromCol) ?: return MoveResult.INVALID

        val captured = board.movePiece(piece, move.toRow, move.toCol)
        if (captured != null) {
            capturedByEnemy++
            if (piece.ability == Ability.EXPLOSION) {
                board.explodeAround(move.toRow, move.toCol, false)
            }
        }

        if (!board.isPlayerKingAlive()) {
            turnState = TurnState.GAME_OVER
            return MoveResult.GAME_OVER
        }

        turnState = TurnState.PLAYER_TURN
        doubleMoveUsed = false
        return MoveResult.MOVE_OK
    }

    fun startNextRound() {
        turnState = TurnState.PLAYER_TURN
        selectedPiece = null
        validMoves = emptyList()
        doubleMoveUsed = false
    }

    fun isValidMove(toRow: Int, toCol: Int): Boolean =
        validMoves.any { it.toRow == toRow && it.toCol == toCol }
}

enum class MoveResult {
    MOVE_OK,
    DOUBLE_MOVE_AVAILABLE,
    ROUND_WON,
    GAME_OVER,
    INVALID
}
