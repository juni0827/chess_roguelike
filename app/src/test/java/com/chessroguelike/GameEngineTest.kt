package com.chessroguelike

import com.chessroguelike.engine.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class GameEngineTest {

    private lateinit var board: ChessBoard

    @Before
    fun setUp() {
        board = ChessBoard()
    }

    // --- Pawn moves ---

    @Test
    fun `pawn moves forward one square`() {
        val pawn = board.createAndAddPiece(PieceType.PAWN, true, 6, 4)
        val moves = MoveGenerator.getValidMoves(pawn, board)
        assertTrue("Pawn should move forward", moves.any { it.toRow == 5 && it.toCol == 4 })
    }

    @Test
    fun `pawn can move two squares from starting row`() {
        val pawn = board.createAndAddPiece(PieceType.PAWN, true, 6, 4)
        val moves = MoveGenerator.getValidMoves(pawn, board)
        assertTrue("Pawn should move 2 from start", moves.any { it.toRow == 4 && it.toCol == 4 })
    }

    @Test
    fun `pawn cannot move two squares when blocked`() {
        board.createAndAddPiece(PieceType.PAWN, true, 6, 4)
        board.createAndAddPiece(PieceType.PAWN, false, 5, 4)
        val pawn = board.getPiece(6, 4)!!
        val moves = MoveGenerator.getValidMoves(pawn, board)
        assertFalse("Pawn should not move when blocked", moves.any { it.toCol == 4 })
    }

    @Test
    fun `pawn captures diagonally`() {
        val pawn = board.createAndAddPiece(PieceType.PAWN, true, 5, 4)
        board.createAndAddPiece(PieceType.PAWN, false, 4, 3)
        board.createAndAddPiece(PieceType.PAWN, false, 4, 5)
        val moves = MoveGenerator.getValidMoves(pawn, board)
        assertTrue("Pawn captures left diagonal", moves.any { it.toRow == 4 && it.toCol == 3 })
        assertTrue("Pawn captures right diagonal", moves.any { it.toRow == 4 && it.toCol == 5 })
    }

    @Test
    fun `enemy pawn moves downward`() {
        val pawn = board.createAndAddPiece(PieceType.PAWN, false, 1, 4)
        val moves = MoveGenerator.getValidMoves(pawn, board)
        assertTrue("Enemy pawn moves down", moves.any { it.toRow == 2 && it.toCol == 4 })
        assertTrue("Enemy pawn moves 2 from start", moves.any { it.toRow == 3 && it.toCol == 4 })
    }

    // --- Knight moves ---

    @Test
    fun `knight moves in L-shape`() {
        val knight = board.createAndAddPiece(PieceType.KNIGHT, true, 4, 4)
        val moves = MoveGenerator.getValidMoves(knight, board)
        val expectedMoves = setOf(
            Pair(2, 3), Pair(2, 5), Pair(3, 2), Pair(3, 6),
            Pair(5, 2), Pair(5, 6), Pair(6, 3), Pair(6, 5)
        )
        expectedMoves.forEach { (r, c) ->
            assertTrue("Knight should move to ($r,$c)", moves.any { it.toRow == r && it.toCol == c })
        }
    }

    @Test
    fun `knight cannot move to own piece square`() {
        val knight = board.createAndAddPiece(PieceType.KNIGHT, true, 4, 4)
        board.createAndAddPiece(PieceType.PAWN, true, 2, 3)
        val moves = MoveGenerator.getValidMoves(knight, board)
        assertFalse("Knight cannot land on own piece", moves.any { it.toRow == 2 && it.toCol == 3 })
    }

    @Test
    fun `knight can jump over pieces`() {
        val knight = board.createAndAddPiece(PieceType.KNIGHT, true, 7, 1)
        // Block surrounding squares
        board.createAndAddPiece(PieceType.PAWN, true, 6, 1)
        board.createAndAddPiece(PieceType.PAWN, true, 7, 0)
        board.createAndAddPiece(PieceType.PAWN, true, 7, 2)
        val moves = MoveGenerator.getValidMoves(knight, board)
        assertTrue("Knight jumps over pieces to (5,0)", moves.any { it.toRow == 5 && it.toCol == 0 })
        assertTrue("Knight jumps over pieces to (5,2)", moves.any { it.toRow == 5 && it.toCol == 2 })
    }

    // --- Rook moves ---

    @Test
    fun `rook moves horizontally and vertically`() {
        val rook = board.createAndAddPiece(PieceType.ROOK, true, 4, 4)
        val moves = MoveGenerator.getValidMoves(rook, board)
        // Should be able to move along row 4
        assertTrue("Rook moves left", moves.any { it.toRow == 4 && it.toCol == 0 })
        assertTrue("Rook moves right", moves.any { it.toRow == 4 && it.toCol == 7 })
        // Should be able to move along col 4
        assertTrue("Rook moves up", moves.any { it.toRow == 0 && it.toCol == 4 })
        assertTrue("Rook moves down", moves.any { it.toRow == 7 && it.toCol == 4 })
    }

    @Test
    fun `rook blocked by own piece`() {
        val rook = board.createAndAddPiece(PieceType.ROOK, true, 4, 4)
        board.createAndAddPiece(PieceType.PAWN, true, 4, 6)
        val moves = MoveGenerator.getValidMoves(rook, board)
        assertTrue("Rook moves to col 5", moves.any { it.toRow == 4 && it.toCol == 5 })
        assertFalse("Rook blocked at col 6", moves.any { it.toRow == 4 && it.toCol == 6 })
        assertFalse("Rook blocked beyond col 6", moves.any { it.toRow == 4 && it.toCol == 7 })
    }

    @Test
    fun `rook can capture enemy piece`() {
        val rook = board.createAndAddPiece(PieceType.ROOK, true, 4, 4)
        board.createAndAddPiece(PieceType.PAWN, false, 4, 6)
        val moves = MoveGenerator.getValidMoves(rook, board)
        assertTrue("Rook captures enemy at col 6", moves.any { it.toRow == 4 && it.toCol == 6 && it.capturedPiece != null })
        assertFalse("Rook cannot go beyond captured piece", moves.any { it.toRow == 4 && it.toCol == 7 })
    }

    // --- Bishop moves ---

    @Test
    fun `bishop moves diagonally`() {
        val bishop = board.createAndAddPiece(PieceType.BISHOP, true, 4, 4)
        val moves = MoveGenerator.getValidMoves(bishop, board)
        assertTrue("Bishop moves up-left", moves.any { it.toRow == 3 && it.toCol == 3 })
        assertTrue("Bishop moves up-right", moves.any { it.toRow == 3 && it.toCol == 5 })
        assertTrue("Bishop moves down-left", moves.any { it.toRow == 5 && it.toCol == 3 })
        assertTrue("Bishop moves down-right", moves.any { it.toRow == 5 && it.toCol == 5 })
        assertTrue("Bishop far diagonal", moves.any { it.toRow == 0 && it.toCol == 0 })
    }

    @Test
    fun `bishop blocked diagonally`() {
        val bishop = board.createAndAddPiece(PieceType.BISHOP, true, 4, 4)
        board.createAndAddPiece(PieceType.PAWN, true, 2, 2)
        val moves = MoveGenerator.getValidMoves(bishop, board)
        assertTrue("Bishop moves to (3,3)", moves.any { it.toRow == 3 && it.toCol == 3 })
        assertFalse("Bishop blocked at (2,2)", moves.any { it.toRow == 2 && it.toCol == 2 })
        assertFalse("Bishop cannot pass (2,2)", moves.any { it.toRow == 1 && it.toCol == 1 })
    }

    // --- Queen moves ---

    @Test
    fun `queen combines rook and bishop moves`() {
        val queen = board.createAndAddPiece(PieceType.QUEEN, true, 4, 4)
        val moves = MoveGenerator.getValidMoves(queen, board)
        // Rook-like
        assertTrue("Queen moves horizontally", moves.any { it.toRow == 4 && it.toCol == 0 })
        assertTrue("Queen moves vertically", moves.any { it.toRow == 0 && it.toCol == 4 })
        // Bishop-like
        assertTrue("Queen moves diagonally", moves.any { it.toRow == 0 && it.toCol == 0 })
        assertTrue("Queen moves diagonally 2", moves.any { it.toRow == 7 && it.toCol == 7 })
    }

    // --- King moves ---

    @Test
    fun `king moves one square in any direction`() {
        val king = board.createAndAddPiece(PieceType.KING, true, 4, 4)
        val moves = MoveGenerator.getValidMoves(king, board)
        assertEquals("King has 8 moves from center", 8, moves.size)
    }

    @Test
    fun `king limited at board edge`() {
        val king = board.createAndAddPiece(PieceType.KING, true, 0, 0)
        val moves = MoveGenerator.getValidMoves(king, board)
        assertEquals("King has 3 moves from corner", 3, moves.size)
    }

    // --- Ability: Double Move ---

    @Test
    fun `double move ability transitions to PLAYER_DOUBLE_MOVE state`() {
        val engine = GameEngine()
        engine.initGame()
        // Give double move to a pawn
        val pawn = engine.board.getPlayerPieces().first { it.type == PieceType.PAWN }
        pawn.ability = Ability.DOUBLE_MOVE

        engine.selectPiece(pawn.row, pawn.col)
        val result = engine.makePlayerMove(pawn.row - 1, pawn.col)
        assertEquals("Double move should be available", MoveResult.DOUBLE_MOVE_AVAILABLE, result)
        assertEquals("State should be PLAYER_DOUBLE_MOVE", TurnState.PLAYER_DOUBLE_MOVE, engine.turnState)
    }

    @Test
    fun `skip double move transitions to ENEMY_TURN`() {
        val engine = GameEngine()
        engine.initGame()
        val pawn = engine.board.getPlayerPieces().first { it.type == PieceType.PAWN }
        pawn.ability = Ability.DOUBLE_MOVE

        engine.selectPiece(pawn.row, pawn.col)
        engine.makePlayerMove(pawn.row - 1, pawn.col)
        engine.skipDoubleMove()
        assertEquals("Should be ENEMY_TURN after skip", TurnState.ENEMY_TURN, engine.turnState)
    }

    // --- Ability: Shield ---

    @Test
    fun `shield prevents capture once`() {
        val attacker = board.createAndAddPiece(PieceType.ROOK, true, 4, 0)
        val defender = board.createAndAddPiece(PieceType.PAWN, false, 4, 4)
        defender.shieldActive = true

        val captured = board.movePiece(attacker, 4, 4)
        assertNull("Shield should block capture", captured)
        assertFalse("Shield should be deactivated", defender.shieldActive)
        assertNotNull("Defender still exists", board.getPiece(4, 4))
    }

    @Test
    fun `shield breaks after blocking one attack`() {
        val attacker = board.createAndAddPiece(PieceType.ROOK, true, 4, 0)
        val defender = board.createAndAddPiece(PieceType.PAWN, false, 4, 3)
        defender.shieldActive = true

        // First attack - blocked
        board.movePiece(attacker, 4, 3)
        assertFalse("Shield broken after first block", defender.shieldActive)

        // Second attack - capture succeeds
        val attacker2 = board.createAndAddPiece(PieceType.ROOK, true, 5, 3)
        val captured = board.movePiece(attacker2, 4, 3)
        assertNotNull("Capture succeeds after shield broken", captured)
    }

    // --- Ability: Explosion ---

    @Test
    fun `explosion removes adjacent enemies`() {
        val attacker = board.createAndAddPiece(PieceType.ROOK, true, 4, 0)
        attacker.ability = Ability.EXPLOSION
        val target = board.createAndAddPiece(PieceType.PAWN, false, 4, 4)
        val adjacent1 = board.createAndAddPiece(PieceType.PAWN, false, 4, 5)
        val adjacent2 = board.createAndAddPiece(PieceType.PAWN, false, 3, 4)
        val notAdjacent = board.createAndAddPiece(PieceType.PAWN, false, 4, 6)

        board.movePiece(attacker, 4, 4)
        board.explodeAround(4, 4, true)

        assertNull("Adjacent piece 1 removed", board.getPiece(4, 5))
        assertNull("Adjacent piece 2 removed", board.getPiece(3, 4))
        assertNotNull("Non-adjacent piece survives", board.getPiece(4, 6))
    }

    // --- Board state ---

    @Test
    fun `initial board has 32 pieces`() {
        board.setupInitialBoard()
        assertEquals("Board should have 32 pieces", 32, board.getAllPieces().size)
    }

    @Test
    fun `initial board has player king alive`() {
        board.setupInitialBoard()
        assertTrue("Player king should be alive", board.isPlayerKingAlive())
    }

    @Test
    fun `initial board has enemy king alive`() {
        board.setupInitialBoard()
        assertTrue("Enemy king should be alive", board.isEnemyKingAlive())
    }

    @Test
    fun `removing player king causes isPlayerKingAlive to return false`() {
        board.setupInitialBoard()
        val king = board.getPlayerPieces().find { it.type == PieceType.KING }!!
        board.removePiece(king)
        assertFalse("Player king should be dead", board.isPlayerKingAlive())
    }
}
