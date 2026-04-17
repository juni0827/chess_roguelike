package com.chessroguelike.engine

import kotlinx.serialization.Serializable

@Serializable
enum class TurnState {
    PLAYER_TURN,
    ENEMY_TURN,
    PLAYER_DOUBLE_MOVE,
    ROUND_WON,
    GAME_OVER
}

@Serializable
enum class MoveResult {
    MOVE_OK,
    DOUBLE_MOVE_AVAILABLE,
    ROUND_WON,
    GAME_OVER,
    INVALID
}
