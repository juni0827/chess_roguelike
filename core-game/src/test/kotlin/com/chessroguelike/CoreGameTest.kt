package com.chessroguelike

import com.chessroguelike.ai.EnemyAIService
import com.chessroguelike.ai.AiService
import com.chessroguelike.engine.ChessBoard
import com.chessroguelike.engine.MoveGenerator
import com.chessroguelike.engine.PieceType
import com.chessroguelike.game.GameAction
import com.chessroguelike.game.GameEvent
import com.chessroguelike.game.GameSession
import com.chessroguelike.game.GameRuntime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoreGameTest {

    @Test
    fun `pawn promotes to queen on final rank`() {
        val board = ChessBoard()
        val pawn = board.createAndAddPiece(PieceType.PAWN, true, 1, 0)

        board.movePiece(pawn, 0, 0)

        assertEquals(PieceType.QUEEN, board.getPiece(0, 0)?.type)
    }

    @Test
    fun `double move keeps player turn after first move`() {
        val session = GameSession.new(TestContentRegistry, EnemyAIService(), seed = 7)
        val pawn = session.board.getPlayerPieces().first { it.type == PieceType.PAWN }
        pawn.abilityId = "ability.double_move"

        session.selectSquare(pawn.row, pawn.col)
        session.selectSquare(pawn.row - 1, pawn.col)

        assertEquals(com.chessroguelike.engine.TurnState.PLAYER_DOUBLE_MOVE, session.state().turnState)
    }

    @Test
    fun `shield prevents one capture`() {
        val board = ChessBoard()
        val attacker = board.createAndAddPiece(PieceType.ROOK, true, 4, 0)
        val defender = board.createAndAddPiece(PieceType.PAWN, false, 4, 4).apply {
            shieldActive = true
        }

        val captured = board.movePiece(attacker, 4, 4)

        assertEquals(null, captured)
        assertFalse(defender.shieldActive)
        assertNotNull(board.getPiece(4, 4))
    }

    @Test
    fun `extra range adds extended knight jumps`() {
        val board = ChessBoard()
        val knight = board.createAndAddPiece(PieceType.KNIGHT, true, 4, 4, "ability.extra_range")

        val moves = MoveGenerator.getValidMoves(knight, board, TestContentRegistry)

        assertTrue(moves.any { it.toRow == 1 && it.toCol == 3 })
    }

    @Test
    fun `save snapshot can restore active run`() {
        val runtime = GameRuntime(TestContentRegistry, EnemyAIService())
        runtime.dispatch(GameAction.StartRun(seed = 1234))
        val initialState = requireNotNull(runtime.currentState())
        val board = ChessBoard(initialState.board)
        val pawn = board.getPlayerPieces().first { it.type == PieceType.PAWN }
        runtime.dispatch(GameAction.SelectSquare(pawn.row, pawn.col))
        runtime.dispatch(GameAction.SelectSquare(pawn.row - 1, pawn.col))

        val snapshot = runtime.snapshot()
        val restored = GameRuntime(TestContentRegistry, EnemyAIService(), snapshot.profile, snapshot.activeMods)
        restored.dispatch(GameAction.ResumeRun(snapshot))

        assertEquals(runtime.currentState(), restored.currentState())
    }

    @Test
    fun `round score only counts captures from the current round`() {
        val noOpAi = object : AiService {
            override fun getBestMove(
                board: ChessBoard,
                contentRegistry: com.chessroguelike.content.ContentRegistry,
                round: Int,
                rng: com.chessroguelike.game.DeterministicRng
            ) = null
        }
        val session = GameSession.new(TestContentRegistry, noOpAi, seed = 11)
        session.board.getAllPieces().forEach { piece ->
            session.board.getPieceById(piece.id)?.let(session.board::removePiece)
        }

        session.board.createAndAddPiece(PieceType.KING, true, 7, 4)
        val playerRook = session.board.createAndAddPiece(PieceType.ROOK, true, 1, 0)
        session.board.createAndAddPiece(PieceType.PAWN, false, 0, 0)

        session.selectSquare(playerRook.row, playerRook.col)
        val firstRoundEvents = session.selectSquare(0, 0)

        assertTrue(firstRoundEvents.any { it is GameEvent.RoundCleared && it.round == 1 })
        assertEquals(110, session.state().score)

        val offeredUpgradeId = session.state().offeredUpgradeIds.first()
        session.applyUpgrade(offeredUpgradeId, playerRook.id)

        session.board.getEnemyPieces().forEach { piece ->
            session.board.getPieceById(piece.id)?.let(session.board::removePiece)
        }

        session.selectSquare(playerRook.row, playerRook.col)
        val secondRoundEvents = session.selectSquare(playerRook.row + 1, playerRook.col)

        assertTrue(secondRoundEvents.any { it is GameEvent.RoundCleared && it.round == 2 })
        assertEquals(310, session.state().score)
    }
}
