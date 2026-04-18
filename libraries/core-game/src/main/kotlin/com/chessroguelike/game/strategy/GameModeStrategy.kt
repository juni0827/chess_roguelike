package com.chessroguelike.game.strategy

import com.chessroguelike.event.EventBus
import com.chessroguelike.game.GameEvent

/**
 * Common interface for all game-mode strategies.
 *
 * A strategy encapsulates **how** the game loop processes each
 * tick.  Implementations may be turn-based, real-time, or any
 * hybrid.  The [GameModeManager] holds the active strategy and
 * can hot-swap it at runtime when synergy conditions are met.
 */
interface GameModeStrategy {
    /** Human-readable identifier for this mode (used in events). */
    val modeName: String

    /** Called once when this strategy becomes the active mode. */
    fun onActivate(context: StrategyContext)

    /** Called once when this strategy is being replaced by another. */
    fun onDeactivate(context: StrategyContext)

    /** Process a player action and return resulting [GameEvent]s. */
    fun handleSelectSquare(
        context: StrategyContext,
        row: Int,
        col: Int
    ): List<GameEvent>

    /** Process a tick/frame (meaningful in real-time mode). */
    fun tick(context: StrategyContext, deltaSeconds: Float): List<GameEvent>
}

/**
 * Turn-based strategy interface.
 *
 * When this strategy is active the game loop processes discrete
 * player/enemy turns following standard chess rules.
 */
interface ITurnBasedStrategy : GameModeStrategy {
    /** Skip the current double-move opportunity. */
    fun skipDoubleMove(context: StrategyContext): List<GameEvent>
}

/**
 * Real-time action strategy interface.
 *
 * When this strategy is active, pieces can move freely, fire
 * weapons, and interact with a physics-like system.  Turn-based
 * rules are suspended.
 */
interface IRealTimeActionStrategy : GameModeStrategy {
    /** Fire the weapon on the given entity toward a target position. */
    fun fireWeapon(
        context: StrategyContext,
        shooterEntityId: Int,
        targetRow: Float,
        targetCol: Float
    ): List<GameEvent>

    /** Move an entity freely in real-time toward a target position. */
    fun moveEntity(
        context: StrategyContext,
        entityId: Int,
        targetX: Float,
        targetY: Float
    ): List<GameEvent>
}
