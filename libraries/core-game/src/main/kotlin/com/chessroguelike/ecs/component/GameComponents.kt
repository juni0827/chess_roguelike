package com.chessroguelike.ecs.component

import com.chessroguelike.ecs.Component
import com.chessroguelike.engine.PieceType

/**
 * Turn-based grid coordinates for a chess piece entity.
 *
 * This component is used when the game is in turn-based mode.
 * An entity with a [GridPosition] participates in the standard
 * chess movement system.
 */
data class GridPosition(
    var row: Int,
    var col: Int
) : Component

/**
 * Real-time world-space coordinates.
 *
 * Used in real-time action mode for physics-based movement,
 * projectile trajectories, etc.
 */
data class Transform(
    var x: Float = 0f,
    var y: Float = 0f,
    var rotation: Float = 0f
) : Component

/**
 * Identifies the type and ownership of a chess piece.
 */
data class PieceInfo(
    val pieceType: PieceType,
    val isPlayer: Boolean
) : Component

/**
 * Tracks whether a piece has moved (relevant for castling, pawn
 * double-advance, etc.).
 */
data class MovementState(
    var hasMoved: Boolean = false
) : Component

/**
 * An ability attached to a piece entity.
 *
 * This component can be dynamically added or removed at runtime,
 * enabling abilities like double-move, explosion, or extra-range
 * to be granted by upgrades or synergies.
 */
data class Ability(
    var abilityId: String = "ability.none"
) : Component

/**
 * A one-hit shield that absorbs the first capture attempt.
 *
 * When [active] is true the next attack is absorbed and the
 * component's active flag is set to false instead of destroying
 * the entity.  The component can later be removed entirely or
 * re-activated by an upgrade.
 */
data class Shield(
    var active: Boolean = true
) : Component

/**
 * A weapon component for real-time action mode.
 *
 * Attaching a [Weapon] to an entity allows it to participate in
 * the real-time shooting system.  Removing the component disables
 * ranged attacks without touching any other entity data.
 */
data class Weapon(
    val weaponId: String,
    val damage: Int = 1,
    val range: Float = 5f,
    val cooldownSeconds: Float = 1f,
    var cooldownRemaining: Float = 0f
) : Component

/**
 * Health pool for entities that can take damage in real-time
 * action mode.
 */
data class Health(
    var current: Int,
    var max: Int
) : Component
