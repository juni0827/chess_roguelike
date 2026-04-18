package com.chessroguelike

import com.chessroguelike.ai.EnemyAIService
import com.chessroguelike.ecs.Entity
import com.chessroguelike.ecs.GameSystem
import com.chessroguelike.ecs.World
import com.chessroguelike.ecs.component.Ability
import com.chessroguelike.ecs.component.GridPosition
import com.chessroguelike.ecs.component.Health
import com.chessroguelike.ecs.component.MovementState
import com.chessroguelike.ecs.component.PieceInfo
import com.chessroguelike.ecs.component.Shield
import com.chessroguelike.ecs.component.Transform
import com.chessroguelike.ecs.component.Weapon
import com.chessroguelike.engine.PieceType
import com.chessroguelike.event.EventBus
import com.chessroguelike.event.OnGameModeChanged
import com.chessroguelike.event.OnPieceDestroyed
import com.chessroguelike.event.OnPieceMoved
import com.chessroguelike.event.OnPieceShot
import com.chessroguelike.event.OnRoundCleared
import com.chessroguelike.game.GameAction
import com.chessroguelike.game.GameEvent
import com.chessroguelike.game.GameRuntime
import com.chessroguelike.game.GameSession
import com.chessroguelike.game.strategy.GameModeManager
import com.chessroguelike.game.strategy.GameModeStrategy
import com.chessroguelike.game.strategy.RealTimeActionStrategy
import com.chessroguelike.game.strategy.StrategyContext
import com.chessroguelike.game.strategy.TurnBasedStrategy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the three architecture patterns:
 * 1. ECS (Entity-Component-System)
 * 2. Strategy & State (GameModeManager / hot-swapping)
 * 3. Event-Driven / Observer (EventBus)
 */
class ArchitecturePatternsTest {

    // ────────────────────────────────────────────────────────
    // 1. ECS tests
    // ────────────────────────────────────────────────────────

    @Test
    fun `ECS - create entity and attach components`() {
        val world = World()
        val entity = world.createEntity()
        world.addComponent(entity, GridPosition(row = 7, col = 4))
        world.addComponent(entity, PieceInfo(PieceType.KING, isPlayer = true))

        val grid = world.getComponent<GridPosition>(entity)
        val info = world.getComponent<PieceInfo>(entity)

        assertNotNull(grid)
        assertEquals(7, grid!!.row)
        assertEquals(4, grid.col)
        assertNotNull(info)
        assertEquals(PieceType.KING, info!!.pieceType)
        assertTrue(info.isPlayer)
    }

    @Test
    fun `ECS - dynamically add and remove components at runtime`() {
        val world = World()
        val entity = world.createEntity()
        world.addComponent(entity, GridPosition(0, 0))
        world.addComponent(entity, PieceInfo(PieceType.PAWN, isPlayer = true))

        // Initially no weapon
        assertFalse(world.hasComponent<Weapon>(entity))

        // Dynamically add a weapon (runtime composition)
        world.addComponent(entity, Weapon(weaponId = "laser", damage = 3, range = 10f))
        assertTrue(world.hasComponent<Weapon>(entity))
        assertEquals("laser", world.getComponent<Weapon>(entity)!!.weaponId)

        // Dynamically remove the weapon
        world.removeComponent<Weapon>(entity)
        assertFalse(world.hasComponent<Weapon>(entity))
    }

    @Test
    fun `ECS - shield component can be toggled independently`() {
        val world = World()
        val entity = world.createEntity()
        world.addComponent(entity, PieceInfo(PieceType.ROOK, isPlayer = false))
        world.addComponent(entity, Shield(active = true))

        val shield = world.getComponent<Shield>(entity)!!
        assertTrue(shield.active)

        // Absorb one hit
        shield.active = false
        assertFalse(world.getComponent<Shield>(entity)!!.active)

        // Remove shield entirely
        world.removeComponent<Shield>(entity)
        assertNull(world.getComponent<Shield>(entity))
    }

    @Test
    fun `ECS - destroy entity removes all components`() {
        val world = World()
        val entity = world.createEntity()
        world.addComponent(entity, GridPosition(3, 3))
        world.addComponent(entity, PieceInfo(PieceType.KNIGHT, isPlayer = true))
        world.addComponent(entity, Ability(abilityId = "ability.double_move"))

        world.destroyEntity(entity)

        assertFalse(world.isAlive(entity))
        assertNull(world.getComponent<GridPosition>(entity))
        assertNull(world.getComponent<PieceInfo>(entity))
        assertNull(world.getComponent<Ability>(entity))
    }

    @Test
    fun `ECS - query entities by component types`() {
        val world = World()
        val king = world.createEntity()
        world.addComponent(king, GridPosition(7, 4))
        world.addComponent(king, PieceInfo(PieceType.KING, isPlayer = true))

        val armedPawn = world.createEntity()
        world.addComponent(armedPawn, GridPosition(6, 0))
        world.addComponent(armedPawn, PieceInfo(PieceType.PAWN, isPlayer = true))
        world.addComponent(armedPawn, Weapon(weaponId = "bow", damage = 1))

        val unarmedPawn = world.createEntity()
        world.addComponent(unarmedPawn, GridPosition(6, 1))
        world.addComponent(unarmedPawn, PieceInfo(PieceType.PAWN, isPlayer = true))

        // Query entities with both GridPosition and Weapon
        val armed = world.query(GridPosition::class, Weapon::class)
        assertEquals(1, armed.size)
        assertEquals(armedPawn, armed.first())

        // Query all entities with GridPosition
        val allPositioned = world.query(GridPosition::class)
        assertEquals(3, allPositioned.size)
    }

    @Test
    fun `ECS - system executes on world tick`() {
        val world = World()
        val entity = world.createEntity()
        world.addComponent(entity, GridPosition(6, 0))
        world.addComponent(entity, MovementState(hasMoved = false))

        // A simple system that marks all entities as having moved
        val markMovedSystem = object : GameSystem {
            override fun update(world: World) {
                world.allOf<MovementState>().forEach { (_, state) ->
                    state.hasMoved = true
                }
            }
        }
        world.addSystem(markMovedSystem)
        world.tick()

        assertTrue(world.getComponent<MovementState>(entity)!!.hasMoved)
    }

    @Test
    fun `ECS - allOf returns matching entity-component pairs`() {
        val world = World()
        val e1 = world.createEntity()
        world.addComponent(e1, Transform(x = 1f, y = 2f))
        val e2 = world.createEntity()
        world.addComponent(e2, Transform(x = 3f, y = 4f))
        val e3 = world.createEntity()
        world.addComponent(e3, GridPosition(0, 0)) // no Transform

        val transforms = world.allOf<Transform>()
        assertEquals(2, transforms.size)
    }

    // ────────────────────────────────────────────────────────
    // 2. Strategy pattern tests
    // ────────────────────────────────────────────────────────

    @Test
    fun `Strategy - TurnBasedStrategy delegates to GameSession`() {
        val eventBus = EventBus()
        val session = GameSession.new(TestContentRegistry, EnemyAIService(), seed = 42, eventBus = eventBus)
        val strategy = TurnBasedStrategy()
        val context = StrategyContext(
            session = session,
            contentRegistry = TestContentRegistry,
            aiService = EnemyAIService(),
            eventBus = eventBus,
            rng = session.rng
        )

        strategy.onActivate(context)

        // Select a player pawn
        val pawn = session.board.getPlayerPieces().first { it.type == PieceType.PAWN }
        val events = strategy.handleSelectSquare(context, pawn.row, pawn.col)

        assertTrue(events.any { it is GameEvent.StateChanged })
    }

    @Test
    fun `Strategy - GameModeManager hot-swaps strategies`() {
        val eventBus = EventBus()
        val session = GameSession.new(TestContentRegistry, EnemyAIService(), seed = 42, eventBus = eventBus)
        val context = StrategyContext(
            session = session,
            contentRegistry = TestContentRegistry,
            aiService = EnemyAIService(),
            eventBus = eventBus,
            rng = session.rng
        )

        val turnBased = TurnBasedStrategy()
        val world = World()
        val realTime = RealTimeActionStrategy(world)
        val manager = GameModeManager(turnBased, context)

        assertEquals("turn_based", manager.activeStrategy.modeName)

        // Hot-swap to real-time action
        val swapEvents = manager.swapStrategy(realTime)
        assertEquals("real_time_action", manager.activeStrategy.modeName)
        assertTrue(swapEvents.any { it is GameEvent.StateChanged })

        // Swap back to turn-based
        manager.swapStrategy(turnBased)
        assertEquals("turn_based", manager.activeStrategy.modeName)
    }

    @Test
    fun `Strategy - mode change fires OnGameModeChanged event`() {
        val eventBus = EventBus()
        val session = GameSession.new(TestContentRegistry, EnemyAIService(), seed = 42, eventBus = eventBus)
        val context = StrategyContext(
            session = session,
            contentRegistry = TestContentRegistry,
            aiService = EnemyAIService(),
            eventBus = eventBus,
            rng = session.rng
        )

        var receivedEvent: OnGameModeChanged? = null
        eventBus.subscribe<OnGameModeChanged> { receivedEvent = it }

        val manager = GameModeManager(TurnBasedStrategy(), context)
        val world = World()
        manager.swapStrategy(RealTimeActionStrategy(world))

        assertNotNull(receivedEvent)
        assertEquals("turn_based", receivedEvent!!.previousMode)
        assertEquals("real_time_action", receivedEvent!!.newMode)
    }

    @Test
    fun `Strategy - RealTimeActionStrategy converts GridPosition to Transform on activate`() {
        val world = World()
        val entity = world.createEntity()
        world.addComponent(entity, GridPosition(row = 5, col = 3))
        world.addComponent(entity, PieceInfo(PieceType.ROOK, isPlayer = true))

        val eventBus = EventBus()
        val session = GameSession.new(TestContentRegistry, EnemyAIService(), seed = 42, eventBus = eventBus)
        val context = StrategyContext(session, TestContentRegistry, EnemyAIService(), eventBus, session.rng)

        val strategy = RealTimeActionStrategy(world)
        strategy.onActivate(context)

        val transform = world.getComponent<Transform>(entity)
        assertNotNull(transform)
        assertEquals(3f, transform!!.x, 0.01f)
        assertEquals(5f, transform.y, 0.01f)
    }

    @Test
    fun `Strategy - RealTimeActionStrategy tick updates weapon cooldowns`() {
        val world = World()
        val entity = world.createEntity()
        world.addComponent(entity, Weapon(weaponId = "gun", damage = 2, cooldownSeconds = 1f, cooldownRemaining = 0.5f))

        val eventBus = EventBus()
        val session = GameSession.new(TestContentRegistry, EnemyAIService(), seed = 42, eventBus = eventBus)
        val context = StrategyContext(session, TestContentRegistry, EnemyAIService(), eventBus, session.rng)

        val strategy = RealTimeActionStrategy(world)
        strategy.tick(context, 0.3f)

        assertEquals(0.2f, world.getComponent<Weapon>(entity)!!.cooldownRemaining, 0.01f)
    }

    @Test
    fun `Strategy - GameRuntime creates GameModeManager on StartRun`() {
        val runtime = GameRuntime(TestContentRegistry, EnemyAIService())
        assertNull(runtime.modeManager)

        runtime.dispatch(GameAction.StartRun(seed = 99))
        assertNotNull(runtime.modeManager)
        assertEquals("turn_based", runtime.modeManager!!.activeStrategy.modeName)
    }

    // ────────────────────────────────────────────────────────
    // 3. Event Bus / Observer pattern tests
    // ────────────────────────────────────────────────────────

    @Test
    fun `EventBus - subscribe and publish`() {
        val bus = EventBus()
        val received = mutableListOf<OnPieceMoved>()

        bus.subscribe<OnPieceMoved> { received.add(it) }
        bus.publish(OnPieceMoved(
            move = com.chessroguelike.engine.Move(6, 0, 5, 0),
            isPlayer = true,
            entityId = 1
        ))

        assertEquals(1, received.size)
        assertTrue(received.first().isPlayer)
    }

    @Test
    fun `EventBus - multiple subscribers receive the same event`() {
        val bus = EventBus()
        var count = 0

        bus.subscribe<OnRoundCleared> { count++ }
        bus.subscribe<OnRoundCleared> { count++ }
        bus.publish(OnRoundCleared(round = 1))

        assertEquals(2, count)
    }

    @Test
    fun `EventBus - unsubscribe stops delivery`() {
        val bus = EventBus()
        var count = 0

        val sub = bus.subscribe<OnRoundCleared> { count++ }
        bus.publish(OnRoundCleared(round = 1))
        assertEquals(1, count)

        bus.unsubscribe(sub)
        bus.publish(OnRoundCleared(round = 2))
        assertEquals(1, count) // no increment
    }

    @Test
    fun `EventBus - different event types are independent`() {
        val bus = EventBus()
        val moves = mutableListOf<OnPieceMoved>()
        val shots = mutableListOf<OnPieceShot>()

        bus.subscribe<OnPieceMoved> { moves.add(it) }
        bus.subscribe<OnPieceShot> { shots.add(it) }

        bus.publish(OnPieceMoved(
            move = com.chessroguelike.engine.Move(1, 0, 2, 0),
            isPlayer = false
        ))
        bus.publish(OnPieceShot(
            shooterEntityId = 1,
            targetEntityId = 2,
            weaponId = "laser",
            damage = 5
        ))

        assertEquals(1, moves.size)
        assertEquals(1, shots.size)
    }

    @Test
    fun `EventBus - clear removes all subscriptions`() {
        val bus = EventBus()
        var count = 0
        bus.subscribe<OnRoundCleared> { count++ }
        bus.subscribe<OnPieceMoved> { count++ }

        bus.clear()
        bus.publish(OnRoundCleared(round = 1))
        bus.publish(OnPieceMoved(
            move = com.chessroguelike.engine.Move(0, 0, 1, 0),
            isPlayer = true
        ))

        assertEquals(0, count)
    }

    @Test
    fun `EventBus - GameSession publishes OnPieceMoved on move`() {
        val eventBus = EventBus()
        val received = mutableListOf<OnPieceMoved>()
        eventBus.subscribe<OnPieceMoved> { received.add(it) }

        val session = GameSession.new(TestContentRegistry, EnemyAIService(), seed = 42, eventBus = eventBus)
        val pawn = session.board.getPlayerPieces().first { it.type == PieceType.PAWN }
        session.selectSquare(pawn.row, pawn.col)
        session.selectSquare(pawn.row - 1, pawn.col)

        // At least one OnPieceMoved should have been published (player move + possibly enemy move)
        assertTrue(received.isNotEmpty())
        assertTrue(received.any { it.isPlayer })
    }

    @Test
    fun `EventBus - publishAll delivers events in order`() {
        val bus = EventBus()
        val rounds = mutableListOf<Int>()
        bus.subscribe<OnRoundCleared> { rounds.add(it.round) }

        bus.publishAll(listOf(
            OnRoundCleared(round = 1),
            OnRoundCleared(round = 2),
            OnRoundCleared(round = 3)
        ))

        assertEquals(listOf(1, 2, 3), rounds)
    }

    // ────────────────────────────────────────────────────────
    // Integration: all three patterns working together
    // ────────────────────────────────────────────────────────

    @Test
    fun `Integration - ECS world with Strategy swap fires events through EventBus`() {
        // Setup ECS world
        val world = World()
        val entity = world.createEntity()
        world.addComponent(entity, GridPosition(row = 7, col = 0))
        world.addComponent(entity, PieceInfo(PieceType.ROOK, isPlayer = true))

        // Setup EventBus
        val eventBus = EventBus()
        val modeChanges = mutableListOf<OnGameModeChanged>()
        eventBus.subscribe<OnGameModeChanged> { modeChanges.add(it) }

        // Setup GameSession and Strategy
        val session = GameSession.new(TestContentRegistry, EnemyAIService(), seed = 42, eventBus = eventBus)
        val context = StrategyContext(session, TestContentRegistry, EnemyAIService(), eventBus, session.rng)

        val turnBased = TurnBasedStrategy()
        val realTime = RealTimeActionStrategy(world)
        val manager = GameModeManager(turnBased, context)

        // Swap turn-based → real-time
        manager.swapStrategy(realTime)
        assertEquals(1, modeChanges.size)
        assertEquals("turn_based", modeChanges[0].previousMode)
        assertEquals("real_time_action", modeChanges[0].newMode)

        // ECS entity should now have a Transform (added on activate)
        assertNotNull(world.getComponent<Transform>(entity))

        // Swap back real-time → turn-based
        manager.swapStrategy(turnBased)
        assertEquals(2, modeChanges.size)
        assertEquals("real_time_action", modeChanges[1].previousMode)
        assertEquals("turn_based", modeChanges[1].newMode)
    }
}
