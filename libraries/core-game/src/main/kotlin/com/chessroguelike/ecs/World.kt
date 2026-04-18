package com.chessroguelike.ecs

import kotlin.reflect.KClass

/**
 * The ECS World – central registry of entities, components and systems.
 *
 * Usage:
 * ```
 * val world = World()
 * val entity = world.createEntity()
 * world.addComponent(entity, GridPosition(row = 7, col = 4))
 * world.addComponent(entity, PieceInfo(PieceType.KING, isPlayer = true))
 * world.addSystem(MovementSystem())
 * world.tick()                       // runs every registered system
 * world.removeComponent<Weapon>(entity)  // runtime detach
 * ```
 */
class World {

    /* ── entity management ─────────────────────────────────── */

    private var nextEntityId = 0
    private val aliveEntities = mutableSetOf<Entity>()

    fun createEntity(): Entity {
        val entity = Entity(nextEntityId++)
        aliveEntities.add(entity)
        return entity
    }

    fun destroyEntity(entity: Entity) {
        aliveEntities.remove(entity)
        componentStores.values.forEach { it.remove(entity.id) }
    }

    fun isAlive(entity: Entity): Boolean = entity in aliveEntities

    fun allEntities(): Set<Entity> = aliveEntities.toSet()

    /* ── component management ──────────────────────────────── */

    // KClass -> (entityId -> Component)
    private val componentStores =
        mutableMapOf<KClass<out Component>, MutableMap<Int, Component>>()

    fun <T : Component> addComponent(entity: Entity, component: T) {
        require(isAlive(entity)) { "Cannot add component to dead entity ${entity.id}" }
        val store = componentStores.getOrPut(component::class) { mutableMapOf() }
        store[entity.id] = component
    }

    fun <T : Component> removeComponent(entity: Entity, type: KClass<T>) {
        componentStores[type]?.remove(entity.id)
    }

    inline fun <reified T : Component> removeComponent(entity: Entity) {
        removeComponent(entity, T::class)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Component> getComponent(entity: Entity, type: KClass<T>): T? {
        return componentStores[type]?.get(entity.id) as? T
    }

    inline fun <reified T : Component> getComponent(entity: Entity): T? {
        return getComponent(entity, T::class)
    }

    fun <T : Component> hasComponent(entity: Entity, type: KClass<T>): Boolean {
        return componentStores[type]?.containsKey(entity.id) == true
    }

    inline fun <reified T : Component> hasComponent(entity: Entity): Boolean {
        return hasComponent(entity, T::class)
    }

    /**
     * Query all entities that have **all** of the requested component types.
     */
    fun query(vararg types: KClass<out Component>): List<Entity> {
        return aliveEntities.filter { entity ->
            types.all { type -> hasComponent(entity, type) }
        }
    }

    /**
     * Query entities that have all requested component types.
     * Returns pairs of (Entity, List<Component>) for convenient access.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Component> allOf(type: KClass<T>): List<Pair<Entity, T>> {
        val store = componentStores[type] ?: return emptyList()
        return store.entries
            .filter { (id, _) -> aliveEntities.contains(Entity(id)) }
            .map { (id, comp) -> Entity(id) to comp as T }
    }

    inline fun <reified T : Component> allOf(): List<Pair<Entity, T>> = allOf(T::class)

    /* ── system management ─────────────────────────────────── */

    private val systems = mutableListOf<GameSystem>()

    fun addSystem(system: GameSystem) {
        systems.add(system)
    }

    fun removeSystem(system: GameSystem) {
        systems.remove(system)
    }

    fun systems(): List<GameSystem> = systems.toList()

    /**
     * Execute one tick – updates every registered system in order.
     */
    fun tick() {
        systems.forEach { it.update(this) }
    }
}
