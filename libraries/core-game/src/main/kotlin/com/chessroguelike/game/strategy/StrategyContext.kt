package com.chessroguelike.game.strategy

import com.chessroguelike.ai.AiService
import com.chessroguelike.content.ContentRegistry
import com.chessroguelike.engine.ChessBoard
import com.chessroguelike.event.EventBus
import com.chessroguelike.game.DeterministicRng
import com.chessroguelike.game.GameSession

/**
 * Shared runtime context passed to every [GameModeStrategy].
 *
 * A strategy never holds mutable game state directly – it reads
 * and writes through this context object so that hot-swapping
 * strategies does not lose any data.
 */
data class StrategyContext(
    val session: GameSession,
    val contentRegistry: ContentRegistry,
    val aiService: AiService,
    val eventBus: EventBus,
    val rng: DeterministicRng
)
