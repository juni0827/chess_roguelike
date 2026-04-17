package com.chessroguelike.game

import com.chessroguelike.ai.AiService
import com.chessroguelike.content.AbilityEffectType
import com.chessroguelike.content.ContentRegistry
import com.chessroguelike.content.UpgradeEffectDefinition
import com.chessroguelike.engine.ChessBoard
import com.chessroguelike.engine.ChessPiece
import com.chessroguelike.engine.Move
import com.chessroguelike.engine.MoveGenerator
import com.chessroguelike.engine.MoveResult
import com.chessroguelike.engine.PieceType
import com.chessroguelike.engine.TurnState

class GameSession private constructor(
    private val contentRegistry: ContentRegistry,
    private val aiService: AiService,
    private val rng: DeterministicRng,
    val board: ChessBoard,
    private var turnState: TurnState,
    private var currentRound: Int,
    private var score: Int,
    private var selectedPieceId: Int?,
    private var validMoves: List<Move>,
    private var capturedByPlayer: Int,
    private var capturedByEnemy: Int,
    private var offeredUpgradeIds: List<String>
) {
    companion object {
        fun new(contentRegistry: ContentRegistry, aiService: AiService, seed: Long): GameSession {
            val board = ChessBoard()
            board.setupInitialBoard()
            return GameSession(
                contentRegistry = contentRegistry,
                aiService = aiService,
                rng = DeterministicRng(seed),
                board = board,
                turnState = TurnState.PLAYER_TURN,
                currentRound = 1,
                score = 0,
                selectedPieceId = null,
                validMoves = emptyList(),
                capturedByPlayer = 0,
                capturedByEnemy = 0,
                offeredUpgradeIds = emptyList()
            )
        }

        fun restore(contentRegistry: ContentRegistry, aiService: AiService, snapshot: ActiveRunSnapshot): GameSession {
            return GameSession(
                contentRegistry = contentRegistry,
                aiService = aiService,
                rng = DeterministicRng(snapshot.rngState),
                board = ChessBoard(snapshot.board),
                turnState = snapshot.turnState,
                currentRound = snapshot.currentRound,
                score = snapshot.score,
                selectedPieceId = snapshot.selectedPieceId,
                validMoves = snapshot.validMoves,
                capturedByPlayer = snapshot.capturedByPlayer,
                capturedByEnemy = snapshot.capturedByEnemy,
                offeredUpgradeIds = snapshot.offeredUpgradeIds
            )
        }
    }

    fun state(): GameState = GameState(
        board = board.snapshot(),
        turnState = turnState,
        currentRound = currentRound,
        maxRounds = contentRegistry.balance.maxRounds,
        score = score,
        selectedPieceId = selectedPieceId,
        validMoves = validMoves,
        capturedByPlayer = capturedByPlayer,
        capturedByEnemy = capturedByEnemy,
        offeredUpgradeIds = offeredUpgradeIds,
        rngState = rng.state
    )

    fun snapshot(): ActiveRunSnapshot = ActiveRunSnapshot(
        board = board.snapshot(),
        turnState = turnState,
        currentRound = currentRound,
        score = score,
        selectedPieceId = selectedPieceId,
        validMoves = validMoves,
        capturedByPlayer = capturedByPlayer,
        capturedByEnemy = capturedByEnemy,
        offeredUpgradeIds = offeredUpgradeIds,
        rngState = rng.state
    )

    fun selectSquare(row: Int, col: Int): List<GameEvent> {
        if (offeredUpgradeIds.isNotEmpty()) {
            return listOf(GameEvent.Notification(com.chessroguelike.content.LocalizedText(com.chessroguelike.content.TextKey("toast.upgrade_pending"))))
        }
        if (turnState != TurnState.PLAYER_TURN && turnState != TurnState.PLAYER_DOUBLE_MOVE) {
            return emptyList()
        }

        if (validMoves.any { it.toRow == row && it.toCol == col }) {
            return handlePlayerMove(row, col)
        }

        val piece = board.getPiece(row, col)
        if (piece == null || !piece.isPlayer) {
            selectedPieceId = null
            validMoves = emptyList()
            return listOf(
                GameEvent.Notification(
                    com.chessroguelike.content.LocalizedText(com.chessroguelike.content.TextKey("toast.select_player_piece"))
                ),
                GameEvent.StateChanged
            )
        }

        selectedPieceId = piece.id
        validMoves = MoveGenerator.getValidMoves(piece, board, contentRegistry)
        return listOf(GameEvent.StateChanged)
    }

    fun skipDoubleMove(): List<GameEvent> {
        if (turnState != TurnState.PLAYER_DOUBLE_MOVE) return emptyList()
        turnState = TurnState.ENEMY_TURN
        selectedPieceId = null
        validMoves = emptyList()
        return performEnemyTurn()
    }

    fun applyUpgrade(upgradeId: String, targetPieceId: Int?): List<GameEvent> {
        if (!offeredUpgradeIds.contains(upgradeId)) return emptyList()
        val definition = contentRegistry.upgradeDefinition(upgradeId)
        when (val effect = definition.effect) {
            is UpgradeEffectDefinition.AddPiece -> {
                val freeSquare = findFreeSquareForPlayer()
                if (freeSquare != null) {
                    board.createAndAddPiece(effect.pieceType, true, freeSquare.first, freeSquare.second)
                }
            }
            is UpgradeEffectDefinition.AddAbility -> {
                val targetId = targetPieceId ?: board.getPlayerPieces().firstOrNull { it.type != PieceType.KING }?.id
                val piece = targetId?.let(board::getPieceById)
                if (piece != null) {
                    piece.abilityId = effect.abilityId
                    if (contentRegistry.abilityDefinition(effect.abilityId).effectType == AbilityEffectType.SHIELD) {
                        piece.shieldActive = true
                    }
                }
            }
            is UpgradeEffectDefinition.Heal -> {
                repeat(effect.pawnCount) {
                    val sq = findFreeSquareForPlayer() ?: return@repeat
                    board.createAndAddPiece(PieceType.PAWN, true, sq.first, sq.second)
                }
                if (effect.extraPiecePool.isNotEmpty()) {
                    val healType = rng.pick(effect.extraPiecePool)
                    val sq = findFreeSquareForPlayer()
                    if (sq != null) {
                        board.createAndAddPiece(healType, true, sq.first, sq.second)
                    }
                }
            }
        }

        offeredUpgradeIds = emptyList()
        startNextRound()
        return listOf(GameEvent.StateChanged, GameEvent.SaveRequired)
    }

    fun runEndedCurrencyAward(victory: Boolean): Int {
        val roundAward = currentRound * contentRegistry.balance.metaCurrencyPerRound
        return if (victory) roundAward + contentRegistry.balance.metaCurrencyPerVictory else roundAward
    }

    private fun handlePlayerMove(toRow: Int, toCol: Int): List<GameEvent> {
        val piece = selectedPieceId?.let(board::getPieceById) ?: return emptyList()
        val move = validMoves.find { it.toRow == toRow && it.toCol == toCol } ?: return emptyList()
        val result = makeMove(piece, move, playerMove = true)
        return when (result) {
            MoveResult.DOUBLE_MOVE_AVAILABLE -> listOf(
                GameEvent.MoveExecuted(move, isPlayer = true),
                GameEvent.Notification(com.chessroguelike.content.LocalizedText(com.chessroguelike.content.TextKey("toast.double_move_available"))),
                GameEvent.StateChanged,
                GameEvent.SaveRequired
            )
            MoveResult.ROUND_WON -> {
                score += contentRegistry.balance.scoreRoundBase * currentRound + contentRegistry.balance.scoreCaptureMultiplier * capturedByPlayer
                if (currentRound >= contentRegistry.balance.maxRounds) {
                    return listOf(
                        GameEvent.MoveExecuted(move, isPlayer = true),
                        GameEvent.RunEnded(
                            victory = true,
                            finalRound = currentRound,
                            score = score,
                            awardedCurrency = runEndedCurrencyAward(true)
                        ),
                        GameEvent.StateChanged,
                        GameEvent.SaveRequired
                    )
                }
                offeredUpgradeIds = generateUpgradeOptions()
                listOf(
                    GameEvent.MoveExecuted(move, isPlayer = true),
                    GameEvent.RoundCleared(currentRound),
                    GameEvent.UpgradeOffered(offeredUpgradeIds),
                    GameEvent.StateChanged,
                    GameEvent.SaveRequired
                )
            }
            MoveResult.GAME_OVER -> listOf(
                GameEvent.MoveExecuted(move, isPlayer = true),
                GameEvent.RunEnded(victory = false, finalRound = currentRound, score = score, awardedCurrency = runEndedCurrencyAward(false)),
                GameEvent.StateChanged,
                GameEvent.SaveRequired
            )
            MoveResult.MOVE_OK -> listOf(GameEvent.MoveExecuted(move, isPlayer = true)) + performEnemyTurn()
            MoveResult.INVALID -> emptyList()
        }
    }

    private fun performEnemyTurn(): List<GameEvent> {
        if (turnState != TurnState.ENEMY_TURN) return listOf(GameEvent.StateChanged, GameEvent.SaveRequired)
        val move = aiService.getBestMove(board, contentRegistry, currentRound, rng)
        if (move == null) {
            turnState = TurnState.ROUND_WON
            score += contentRegistry.balance.scoreRoundBase * currentRound + contentRegistry.balance.scoreCaptureMultiplier * capturedByPlayer
            if (currentRound >= contentRegistry.balance.maxRounds) {
                return listOf(
                    GameEvent.RunEnded(
                        victory = true,
                        finalRound = currentRound,
                        score = score,
                        awardedCurrency = runEndedCurrencyAward(true)
                    ),
                    GameEvent.StateChanged,
                    GameEvent.SaveRequired
                )
            }
            offeredUpgradeIds = generateUpgradeOptions()
            return listOf(
                GameEvent.RoundCleared(currentRound),
                GameEvent.UpgradeOffered(offeredUpgradeIds),
                GameEvent.StateChanged,
                GameEvent.SaveRequired
            )
        }
        val piece = board.getPiece(move.fromRow, move.fromCol) ?: return emptyList()
        val result = makeMove(piece, move, playerMove = false)
        return when (result) {
            MoveResult.GAME_OVER -> listOf(
                GameEvent.MoveExecuted(move, isPlayer = false),
                GameEvent.RunEnded(victory = false, finalRound = currentRound, score = score, awardedCurrency = runEndedCurrencyAward(false)),
                GameEvent.StateChanged,
                GameEvent.SaveRequired
            )
            else -> listOf(GameEvent.MoveExecuted(move, isPlayer = false), GameEvent.StateChanged, GameEvent.SaveRequired)
        }
    }

    private fun makeMove(piece: ChessPiece, move: Move, playerMove: Boolean): MoveResult {
        val captured = board.movePiece(piece, move.toRow, move.toCol)
        if (captured != null) {
            if (playerMove) {
                capturedByPlayer++
            } else {
                capturedByEnemy++
            }
            if (contentRegistry.abilityDefinition(piece.abilityId).effectType == AbilityEffectType.EXPLOSION) {
                board.explodeAround(move.toRow, move.toCol, playerMove)
            }
        }

        selectedPieceId = null
        validMoves = emptyList()

        if (!board.isEnemyKingAlive()) {
            turnState = TurnState.ROUND_WON
            return MoveResult.ROUND_WON
        }
        if (!board.isPlayerKingAlive()) {
            turnState = TurnState.GAME_OVER
            return MoveResult.GAME_OVER
        }

        return if (playerMove) {
            val abilityEffect = contentRegistry.abilityDefinition(piece.abilityId).effectType
            if (turnState == TurnState.PLAYER_DOUBLE_MOVE) {
                turnState = TurnState.ENEMY_TURN
                MoveResult.MOVE_OK
            } else if (abilityEffect == AbilityEffectType.DOUBLE_MOVE) {
                turnState = TurnState.PLAYER_DOUBLE_MOVE
                selectedPieceId = piece.id
                validMoves = MoveGenerator.getValidMoves(piece, board, contentRegistry)
                MoveResult.DOUBLE_MOVE_AVAILABLE
            } else {
                turnState = TurnState.ENEMY_TURN
                MoveResult.MOVE_OK
            }
        } else {
            turnState = TurnState.PLAYER_TURN
            MoveResult.MOVE_OK
        }
    }

    private fun startNextRound() {
        currentRound++
        if (currentRound > contentRegistry.balance.maxRounds) {
            turnState = TurnState.ROUND_WON
            return
        }
        turnState = TurnState.PLAYER_TURN
        selectedPieceId = null
        validMoves = emptyList()
        generateEnemySetup()
    }

    private fun generateEnemySetup() {
        board.getAllPieces().filter { !it.isPlayer }.forEach { piece ->
            board.getPieceById(piece.id)?.let(board::removePiece)
        }

        val backRowTypes = listOf(
            PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP, PieceType.QUEEN,
            PieceType.KING, PieceType.BISHOP, PieceType.KNIGHT, PieceType.ROOK
        )
        backRowTypes.forEachIndexed { col, type ->
            board.createAndAddPiece(type, false, 0, col)
        }
        repeat(ChessBoard.SIZE) { col ->
            board.createAndAddPiece(PieceType.PAWN, false, 1, col)
        }

        val roundDef = contentRegistry.roundDefinition(currentRound)
        roundDef.bonusEnemyPieceType?.let { bonusType ->
            val freeCol = (0 until ChessBoard.SIZE).firstOrNull { board.getPiece(2, it) == null }
            if (freeCol != null) {
                board.createAndAddPiece(bonusType, false, 2, freeCol)
            }
        }

        if (roundDef.enemyAbilityIds.isNotEmpty()) {
            val enemyPieces = rng.shuffled(board.getEnemyPieces().filter { it.type != PieceType.KING })
            roundDef.enemyAbilityIds.zip(enemyPieces).forEach { (abilityId, piece) ->
                piece.abilityId = abilityId
                if (contentRegistry.abilityDefinition(abilityId).effectType == AbilityEffectType.SHIELD) {
                    piece.shieldActive = true
                }
            }
        }
    }

    private fun generateUpgradeOptions(): List<String> {
        val pool = contentRegistry.upgrades.values.toList()
        val weighted = pool.flatMap { definition -> List(definition.weight) { definition.id } }
        return rng.shuffled(weighted)
            .distinct()
            .take(contentRegistry.balance.upgradeChoiceCount)
    }

    private fun findFreeSquareForPlayer(): Pair<Int, Int>? {
        for (row in contentRegistry.balance.playerSpawnRows) {
            for (col in 0 until ChessBoard.SIZE) {
                if (board.getPiece(row, col) == null) {
                    return row to col
                }
            }
        }
        return null
    }
}
