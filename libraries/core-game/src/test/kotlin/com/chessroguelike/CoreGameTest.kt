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
import com.chessroguelike.game.RunPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoreGameTest {
    @Test
    fun `king side castling is generated and executed`() {
        val board = ChessBoard()
        val king = board.createAndAddPiece(PieceType.KING, true, 7, 4)
        val rook = board.createAndAddPiece(PieceType.ROOK, true, 7, 7)
        board.createAndAddPiece(PieceType.KING, false, 0, 4)

        val moves = MoveGenerator.getValidMoves(king, board, TestContentRegistry)
        val castle = moves.find { it.toRow == 7 && it.toCol == 6 && it.castleRookFromCol == 7 }
        assertNotNull(castle)

        board.executeMove(king, requireNotNull(castle))

        assertEquals(6, king.col)
        assertEquals(5, rook.col)
        assertTrue(king.hasMoved)
        assertTrue(rook.hasMoved)
    }

    @Test
    fun `en passant is available only immediately`() {
        val board = ChessBoard()
        val whitePawn = board.createAndAddPiece(PieceType.PAWN, true, 3, 4)
        val blackPawn = board.createAndAddPiece(PieceType.PAWN, false, 1, 5)
        board.createAndAddPiece(PieceType.KING, true, 7, 4)
        board.createAndAddPiece(PieceType.KING, false, 0, 4)

        board.movePiece(blackPawn, 3, 5)
        val immediateMoves = MoveGenerator.getValidMoves(whitePawn, board, TestContentRegistry)
        val enPassantMove = immediateMoves.find { it.toRow == 2 && it.toCol == 5 && it.enPassantCaptureId == blackPawn.id }
        assertNotNull(enPassantMove)

        board.executeMove(whitePawn, requireNotNull(enPassantMove))
        assertEquals(null, board.getPieceById(blackPawn.id))
        assertEquals(2, whitePawn.row)
        assertEquals(5, whitePawn.col)
    }

    @Test
    fun `move generator rejects moves that leave king in check`() {
        val board = ChessBoard()
        val king = board.createAndAddPiece(PieceType.KING, true, 7, 4)
        val blocker = board.createAndAddPiece(PieceType.ROOK, true, 7, 3)
        board.createAndAddPiece(PieceType.ROOK, false, 7, 0)
        board.createAndAddPiece(PieceType.KING, false, 0, 4)

        val blockerMoves = MoveGenerator.getValidMoves(blocker, board, TestContentRegistry)

        assertTrue(blockerMoves.none { it.toRow == 6 && it.toCol == 3 })
        assertTrue(MoveGenerator.getValidMoves(king, board, TestContentRegistry).isNotEmpty())
    }


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
        assertEquals(RunPhase.PLAYER_DOUBLE_INPUT, session.state().phase)
    }

    @Test
    fun `runtime phase follows state machine transitions`() {
        val runtime = GameRuntime(TestContentRegistry, EnemyAIService())
        runtime.dispatch(GameAction.StartRun(seed = 333))
        assertEquals(RunPhase.PLAYER_INPUT, runtime.currentPhase())

        val state = requireNotNull(runtime.currentState())
        val board = ChessBoard(state.board)
        val pawn = board.getPlayerPieces().first { it.type == PieceType.PAWN }

        runtime.dispatch(GameAction.SelectSquare(pawn.row, pawn.col))
        runtime.dispatch(GameAction.SelectSquare(pawn.row - 1, pawn.col))

        assertEquals(RunPhase.PLAYER_INPUT, runtime.currentPhase())
    }

    @Test
    fun `runtime rejects out of phase actions`() {
        val runtime = GameRuntime(TestContentRegistry, EnemyAIService())

        assertTrue(runtime.dispatch(GameAction.SelectSquare(6, 0)).isEmpty())

        runtime.dispatch(GameAction.StartRun(seed = 444))
        assertTrue(runtime.dispatch(GameAction.ChooseUpgrade("upgrade.heal")).isEmpty())
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

    @Test
    fun `round start repositions enemy king away from immediate trivial capture`() {
        val noOpAi = object : AiService {
            override fun getBestMove(
                board: ChessBoard,
                contentRegistry: com.chessroguelike.content.ContentRegistry,
                round: Int,
                rng: com.chessroguelike.game.DeterministicRng
            ) = null
        }
        val session = GameSession.new(TestContentRegistry, noOpAi, seed = 17)
        session.board.getAllPieces().forEach { piece ->
            session.board.getPieceById(piece.id)?.let(session.board::removePiece)
        }
        session.board.createAndAddPiece(PieceType.KING, true, 7, 4)
        val playerQueen = session.board.createAndAddPiece(PieceType.QUEEN, true, 1, 4)
        session.board.createAndAddPiece(PieceType.KING, false, 0, 4)

        val events = session.selectSquare(playerQueen.row, playerQueen.col).let { session.selectSquare(0, 4) }
        assertTrue(events.any { it is GameEvent.RoundCleared })

        val offeredUpgradeId = session.state().offeredUpgradeIds.first()
        session.applyUpgrade(offeredUpgradeId, null)

        val enemyKing = session.board.getEnemyPieces().first { it.type == PieceType.KING }
        val capturesEnemyKing = session.board.getPlayerPieces().any { piece ->
            MoveGenerator.getValidMoves(piece, session.board, TestContentRegistry)
                .any { it.toRow == enemyKing.row && it.toCol == enemyKing.col }
        }
        assertFalse(capturesEnemyKing)
    }
}
