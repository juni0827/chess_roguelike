package com.chessroguelike.event

import com.chessroguelike.ecs.Entity
import com.chessroguelike.engine.Move

/**
 * Base type for all domain events flowing through the [EventBus].
 *
 * Core game systems (ECS, Strategy) only **publish** these events.
 * Rendering, UI, sound-effects and other presentation systems
 * **subscribe** to them, keeping logic and view fully decoupled.
 */
sealed interface GameDomainEvent

/* ── piece-related events ──────────────────────────────────── */

/**
 * Fired after a piece has been moved on the board.
 * In turn-based mode this carries the [Move]; in real-time mode
 * the entity's [com.chessroguelike.ecs.component.Transform] is
 * updated instead and this event carries the entity reference.
 */
data class OnPieceMoved(
    val move: Move,
    val isPlayer: Boolean,
    val entityId: Int = Entity.NONE.id
) : GameDomainEvent

/**
 * Fired when a piece entity is captured / destroyed.
 */
data class OnPieceDestroyed(
    val entityId: Int,
    val destroyedByPlayer: Boolean,
    val row: Int,
    val col: Int
) : GameDomainEvent

/**
 * Fired when a piece fires a ranged weapon (real-time action
 * mode).
 */
data class OnPieceShot(
    val shooterEntityId: Int,
    val targetEntityId: Int,
    val weaponId: String,
    val damage: Int
) : GameDomainEvent

/* ── round / game flow events ──────────────────────────────── */

/**
 * Fired when the current round is cleared.
 */
data class OnRoundCleared(val round: Int) : GameDomainEvent

/**
 * Fired when a run ends (victory or defeat).
 */
data class OnRunEnded(
    val victory: Boolean,
    val finalRound: Int,
    val score: Int,
    val awardedCurrency: Int
) : GameDomainEvent

/**
 * Fired when upgrades are offered to the player.
 */
data class OnUpgradeOffered(val upgradeIds: List<String>) : GameDomainEvent

/**
 * Fired when the game mode switches between strategies.
 */
data class OnGameModeChanged(
    val previousMode: String,
    val newMode: String
) : GameDomainEvent

/**
 * Fired when a shield absorbs an attack.
 */
data class OnShieldAbsorbed(
    val entityId: Int,
    val row: Int,
    val col: Int
) : GameDomainEvent

/**
 * Fired when an explosion occurs.
 */
data class OnExplosion(
    val row: Int,
    val col: Int,
    val isPlayerAttacker: Boolean
) : GameDomainEvent
