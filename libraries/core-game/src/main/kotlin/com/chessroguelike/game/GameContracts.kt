package com.chessroguelike.game

import com.chessroguelike.content.LocalizedText
import com.chessroguelike.engine.BoardSnapshot
import com.chessroguelike.engine.Move
import com.chessroguelike.engine.TurnState
import kotlinx.serialization.Serializable

@Serializable
data class GameState(
    val board: BoardSnapshot,
    val turnState: TurnState,
    val currentRound: Int,
    val maxRounds: Int,
    val score: Int,
    val selectedPieceId: Int?,
    val validMoves: List<Move>,
    val capturedByPlayer: Int,
    val capturedByEnemy: Int,
    val offeredUpgradeIds: List<String>,
    val rngState: Long
)

sealed interface GameAction {
    data class StartRun(val seed: Long = System.currentTimeMillis()) : GameAction
    data class ResumeRun(val snapshot: SaveSnapshot) : GameAction
    data class SelectSquare(val row: Int, val col: Int) : GameAction
    data object SkipDoubleMove : GameAction
    data class ChooseUpgrade(val upgradeId: String, val targetPieceId: Int? = null) : GameAction
    data class SpendMetaCurrency(val nodeId: String) : GameAction
    data object AbandonRun : GameAction
}

sealed interface GameEvent {
    data class Notification(val message: LocalizedText) : GameEvent
    data class MoveExecuted(val move: Move, val isPlayer: Boolean) : GameEvent
    data class UpgradeOffered(val upgradeIds: List<String>) : GameEvent
    data class RoundCleared(val round: Int) : GameEvent
    data class RunEnded(
        val victory: Boolean,
        val finalRound: Int,
        val score: Int,
        val awardedCurrency: Int
    ) : GameEvent
    data object SaveRequired : GameEvent
    data object StateChanged : GameEvent
}

@Serializable
data class ActiveRunSnapshot(
    val board: BoardSnapshot,
    val turnState: TurnState,
    val currentRound: Int,
    val score: Int,
    val selectedPieceId: Int?,
    val validMoves: List<Move>,
    val capturedByPlayer: Int,
    val capturedByPlayerThisRound: Int = 0,
    val capturedByEnemy: Int,
    val offeredUpgradeIds: List<String>,
    val rngState: Long
)

@Serializable
data class SettingsState(
    val selectedLocale: String = "ko-KR",
    val enabledMods: List<String> = emptyList()
)

@Serializable
data class ProfileStats(
    val completedRuns: Int = 0,
    val victories: Int = 0,
    val highestRound: Int = 0,
    val highScore: Int = 0
)

@Serializable
data class ProfileState(
    val currency: Int = 0,
    val unlockedNodes: Set<String> = emptySet(),
    val stats: ProfileStats = ProfileStats(),
    val settings: SettingsState = SettingsState()
)

@Serializable
data class ActiveModSnapshot(
    val id: String,
    val version: String,
    val contentHash: String
)

@Serializable
data class SaveSnapshot(
    val version: Int = 1,
    val activeRun: ActiveRunSnapshot? = null,
    val profile: ProfileState = ProfileState(),
    val activeMods: List<ActiveModSnapshot> = emptyList(),
    val contentHash: String = ""
)
