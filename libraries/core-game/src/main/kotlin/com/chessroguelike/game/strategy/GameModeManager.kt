package com.chessroguelike.game.strategy

import com.chessroguelike.event.EventBus
import com.chessroguelike.event.OnGameModeChanged
import com.chessroguelike.game.GameEvent

/**
 * Manages the currently active [GameModeStrategy] and enables
 * **hot-swapping** at runtime.
 *
 * Call [swapStrategy] to atomically deactivate the current
 * strategy and activate a new one.  This replaces hard-coded
 * `if-else` branches in the main loop with a clean polymorphic
 * dispatch.
 *
 * ```
 * val manager = GameModeManager(turnBased, context)
 * // later, when a synergy triggers:
 * manager.swapStrategy(realTimeAction)
 * ```
 */
class GameModeManager(
    initialStrategy: GameModeStrategy,
    private val context: StrategyContext
) {
    var activeStrategy: GameModeStrategy = initialStrategy
        private set

    init {
        activeStrategy.onActivate(context)
    }

    /**
     * Replace the current strategy with [newStrategy].
     *
     * 1. Calls [GameModeStrategy.onDeactivate] on the old strategy.
     * 2. Publishes an [OnGameModeChanged] event.
     * 3. Calls [GameModeStrategy.onActivate] on the new strategy.
     */
    fun swapStrategy(newStrategy: GameModeStrategy): List<GameEvent> {
        val previousMode = activeStrategy.modeName
        activeStrategy.onDeactivate(context)
        activeStrategy = newStrategy
        activeStrategy.onActivate(context)

        context.eventBus.publish(
            OnGameModeChanged(
                previousMode = previousMode,
                newMode = newStrategy.modeName
            )
        )
        return listOf(GameEvent.StateChanged)
    }

    /* ── delegation helpers ────────────────────────────────── */

    fun handleSelectSquare(row: Int, col: Int): List<GameEvent> =
        activeStrategy.handleSelectSquare(context, row, col)

    fun tick(deltaSeconds: Float): List<GameEvent> =
        activeStrategy.tick(context, deltaSeconds)
}
