package com.chessroguelike.game.strategy

import com.chessroguelike.ecs.World
import com.chessroguelike.ecs.component.GridPosition
import com.chessroguelike.ecs.component.Health
import com.chessroguelike.ecs.component.PieceInfo
import com.chessroguelike.ecs.component.Transform
import com.chessroguelike.ecs.component.Weapon
import com.chessroguelike.engine.Move
import com.chessroguelike.event.OnPieceShot
import com.chessroguelike.game.GameEvent

/**
 * Skeleton real-time action strategy.
 *
 * When activated the game switches from discrete turns to
 * continuous real-time processing.  Entities with a
 * [Transform] are moved every frame; entities with a [Weapon]
 * may fire at targets.
 *
 * This is a **scaffold** implementation demonstrating how the
 * Strategy pattern enables hot-swapping the entire game loop
 * without changing any calling code.
 */
class RealTimeActionStrategy(private val world: World) : IRealTimeActionStrategy {

    override val modeName: String = "real_time_action"

    override fun onActivate(context: StrategyContext) {
        // Transition: convert GridPosition components into
        // Transform components so entities can move continuously.
        world.allOf<GridPosition>().forEach { (entity, grid) ->
            if (!world.hasComponent<Transform>(entity)) {
                world.addComponent(
                    entity,
                    Transform(
                        x = grid.col.toFloat(),
                        y = grid.row.toFloat()
                    )
                )
            }
        }
    }

    override fun onDeactivate(context: StrategyContext) {
        // Transition back: snap Transform positions to the
        // nearest grid cell and update GridPosition.
        world.allOf<Transform>().forEach { (entity, transform) ->
            val grid = world.getComponent<GridPosition>(entity)
            if (grid != null) {
                grid.row = transform.y.toInt()
                grid.col = transform.x.toInt()
            }
        }
    }

    override fun handleSelectSquare(
        context: StrategyContext,
        row: Int,
        col: Int
    ): List<GameEvent> {
        // In real-time mode, square selection could mean "set
        // move target" – delegate to moveEntity with cell coords.
        return emptyList()
    }

    override fun fireWeapon(
        context: StrategyContext,
        shooterEntityId: Int,
        targetRow: Float,
        targetCol: Float
    ): List<GameEvent> {
        val shooterEntity = com.chessroguelike.ecs.Entity(shooterEntityId)
        val weapon = world.getComponent<Weapon>(shooterEntity) ?: return emptyList()
        if (weapon.cooldownRemaining > 0f) return emptyList()

        // Find closest enemy entity near the target coordinates.
        val targetEntity = world.allOf<Transform>()
            .filter { (e, _) ->
                val info = world.getComponent<PieceInfo>(e)
                val shooterInfo = world.getComponent<PieceInfo>(shooterEntity)
                info != null && shooterInfo != null && info.isPlayer != shooterInfo.isPlayer
            }
            .minByOrNull { (_, t) ->
                val dx = t.x - targetCol
                val dy = t.y - targetRow
                dx * dx + dy * dy
            }

        if (targetEntity != null) {
            val (target, _) = targetEntity
            val health = world.getComponent<Health>(target)
            if (health != null) {
                health.current -= weapon.damage
                if (health.current <= 0) {
                    world.destroyEntity(target)
                }
            }
            weapon.cooldownRemaining = weapon.cooldownSeconds

            context.eventBus.publish(
                OnPieceShot(
                    shooterEntityId = shooterEntity.id,
                    targetEntityId = target.id,
                    weaponId = weapon.weaponId,
                    damage = weapon.damage
                )
            )
            return listOf(GameEvent.StateChanged)
        }
        return emptyList()
    }

    override fun moveEntity(
        context: StrategyContext,
        entityId: Int,
        targetX: Float,
        targetY: Float
    ): List<GameEvent> {
        val entity = com.chessroguelike.ecs.Entity(entityId)
        val transform = world.getComponent<Transform>(entity) ?: return emptyList()
        transform.x = targetX
        transform.y = targetY
        return listOf(GameEvent.StateChanged)
    }

    override fun tick(context: StrategyContext, deltaSeconds: Float): List<GameEvent> {
        // Update weapon cooldowns.
        world.allOf<Weapon>().forEach { (_, weapon) ->
            if (weapon.cooldownRemaining > 0f) {
                weapon.cooldownRemaining = (weapon.cooldownRemaining - deltaSeconds).coerceAtLeast(0f)
            }
        }
        return emptyList()
    }
}
