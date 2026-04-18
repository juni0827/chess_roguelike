package com.chessroguelike.game.strategy

import com.chessroguelike.event.OnPieceMoved
import com.chessroguelike.game.GameEvent

/**
 * Default turn-based strategy that delegates to the existing
 * [com.chessroguelike.game.GameSession] logic.
 *
 * This is the canonical chess-style mode: players and enemies
 * take turns; movement follows standard chess rules.
 */
class TurnBasedStrategy : ITurnBasedStrategy {

    override val modeName: String = "turn_based"

    override fun onActivate(context: StrategyContext) {
        // No special activation needed for turn-based mode.
    }

    override fun onDeactivate(context: StrategyContext) {
        // No special cleanup needed for turn-based mode.
    }

    override fun handleSelectSquare(
        context: StrategyContext,
        row: Int,
        col: Int
    ): List<GameEvent> {
        val events = context.session.selectSquare(row, col)
        // Publish move events to the domain event bus for view-layer subscribers.
        events.forEach { event ->
            if (event is GameEvent.MoveExecuted) {
                context.eventBus.publish(
                    OnPieceMoved(
                        move = event.move,
                        isPlayer = event.isPlayer
                    )
                )
            }
        }
        return events
    }

    override fun skipDoubleMove(context: StrategyContext): List<GameEvent> {
        return context.session.skipDoubleMove()
    }

    override fun tick(context: StrategyContext, deltaSeconds: Float): List<GameEvent> {
        // Turn-based mode has no per-frame tick processing.
        return emptyList()
    }
}
