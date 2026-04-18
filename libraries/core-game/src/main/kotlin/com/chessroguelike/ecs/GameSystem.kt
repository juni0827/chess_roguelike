package com.chessroguelike.ecs

/**
 * A System operates on entities that possess a specific set of
 * components.
 *
 * Systems contain **all** the behaviour; entities and components
 * are pure data.  The [World] calls [update] once per tick.
 */
interface GameSystem {
    /** Process one logical tick of this system within the given [world]. */
    fun update(world: World)
}
