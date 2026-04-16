package com.chessroguelike.engine

class ChessBoard {
    private val pieces = mutableListOf<ChessPiece>()
    private var nextId = 1

    companion object {
        const val SIZE = 8
    }

    fun getPiece(row: Int, col: Int): ChessPiece? =
        pieces.find { it.row == row && it.col == col }

    fun isValidPosition(row: Int, col: Int): Boolean =
        row in 0 until SIZE && col in 0 until SIZE

    fun getPlayerPieces(): List<ChessPiece> = pieces.filter { it.isPlayer }

    fun getEnemyPieces(): List<ChessPiece> = pieces.filter { !it.isPlayer }

    fun getAllPieces(): List<ChessPiece> = pieces.toList()

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
        return captured
    }

    fun removePiece(piece: ChessPiece) {
        pieces.remove(piece)
    }

    fun addPiece(piece: ChessPiece) {
        pieces.add(piece)
    }

    fun createAndAddPiece(type: PieceType, isPlayer: Boolean, row: Int, col: Int, ability: Ability = Ability.NONE): ChessPiece {
        val piece = ChessPiece(nextId++, type, isPlayer, row, col, ability)
        pieces.add(piece)
        return piece
    }

    fun isPlayerKingAlive(): Boolean = pieces.any { it.isPlayer && it.type == PieceType.KING }

    fun isEnemyKingAlive(): Boolean = pieces.any { !it.isPlayer && it.type == PieceType.KING }

    fun setupInitialBoard() {
        pieces.clear()
        nextId = 1

        // Enemy back row (row 0)
        val backRowTypes = listOf(
            PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP, PieceType.QUEEN,
            PieceType.KING, PieceType.BISHOP, PieceType.KNIGHT, PieceType.ROOK
        )
        backRowTypes.forEachIndexed { col, type ->
            createAndAddPiece(type, false, 0, col)
        }
        // Enemy pawns (row 1)
        repeat(SIZE) { col ->
            createAndAddPiece(PieceType.PAWN, false, 1, col)
        }

        // Player pawns (row 6)
        repeat(SIZE) { col ->
            createAndAddPiece(PieceType.PAWN, true, 6, col)
        }
        // Player back row (row 7)
        backRowTypes.forEachIndexed { col, type ->
            createAndAddPiece(type, true, 7, col)
        }
    }

    fun explodeAround(row: Int, col: Int, isPlayerAttacker: Boolean) {
        val toRemove = pieces.filter { piece ->
            piece.isPlayer != isPlayerAttacker &&
                    Math.abs(piece.row - row) <= 1 &&
                    Math.abs(piece.col - col) <= 1 &&
                    !(piece.row == row && piece.col == col)
        }
        toRemove.forEach { piece ->
            if (piece.shieldActive) {
                piece.shieldActive = false
            } else {
                pieces.remove(piece)
            }
        }
    }

    fun getNextId(): Int = nextId++
}
