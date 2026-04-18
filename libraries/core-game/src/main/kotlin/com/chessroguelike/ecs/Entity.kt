package com.chessroguelike.ecs

/**
 * A lightweight identifier for a game object.
 *
 * In the ECS pattern an Entity is nothing more than a unique ID.
 * All data is stored externally in [Component] instances managed by
 * the [World].  Components can be attached or detached at runtime,
 * allowing a chess piece to dynamically gain a [com.chessroguelike.ecs.component.Weapon]
 * or lose its [com.chessroguelike.ecs.component.Shield] without
 * any change to the Entity itself.
 */
@JvmInline
value class Entity(val id: Int) {
    companion object {
        /** Sentinel that represents "no entity". */
        val NONE = Entity(-1)
    }
}
