package com.chessroguelike.game

import com.chessroguelike.ai.AiService
import com.chessroguelike.content.ContentRegistry
import com.chessroguelike.event.EventBus
import com.chessroguelike.game.strategy.GameModeManager
import com.chessroguelike.game.strategy.GameModeStrategy
import com.chessroguelike.game.strategy.StrategyContext
import com.chessroguelike.game.strategy.TurnBasedStrategy

class GameRuntime(
    private val contentRegistry: ContentRegistry,
    private val aiService: AiService,
    profileState: ProfileState = ProfileState(),
    private val activeMods: List<ActiveModSnapshot> = emptyList(),
    /** Shared event bus for Observer-pattern decoupling. */
    val eventBus: EventBus = EventBus()
) {
    var profile: ProfileState = profileState
        private set

    private var session: GameSession? = null

    /**
     * The [GameModeManager] for Strategy-pattern hot-swapping.
     *
     * Created lazily when a run starts.  Access it to swap the
     * active strategy (e.g. from turn-based to real-time action)
     * via [GameModeManager.swapStrategy].
     */
    var modeManager: GameModeManager? = null
        private set

    fun currentState(): GameState? = session?.state()

    fun dispatch(action: GameAction): List<GameEvent> {
        return when (action) {
            is GameAction.StartRun -> {
                val newSession = GameSession.new(contentRegistry, aiService, action.seed, eventBus)
                session = newSession
                val strategy: GameModeStrategy = TurnBasedStrategy()
                modeManager = GameModeManager(
                    initialStrategy = strategy,
                    context = StrategyContext(
                        session = newSession,
                        contentRegistry = contentRegistry,
                        aiService = aiService,
                        eventBus = eventBus,
                        rng = newSession.rng
                    )
                )
                listOf(GameEvent.StateChanged, GameEvent.SaveRequired)
            }
            is GameAction.ResumeRun -> {
                profile = action.snapshot.profile
                val restored = action.snapshot.activeRun?.let {
                    GameSession.restore(contentRegistry, aiService, it, eventBus)
                }
                session = restored
                if (restored != null) {
                    val strategy: GameModeStrategy = TurnBasedStrategy()
                    modeManager = GameModeManager(
                        initialStrategy = strategy,
                        context = StrategyContext(
                            session = restored,
                            contentRegistry = contentRegistry,
                            aiService = aiService,
                            eventBus = eventBus,
                            rng = restored.rng
                        )
                    )
                }
                listOf(GameEvent.StateChanged)
            }
            is GameAction.SelectSquare -> session?.selectSquare(action.row, action.col) ?: emptyList()
            GameAction.SkipDoubleMove -> session?.skipDoubleMove() ?: emptyList()
            is GameAction.ChooseUpgrade -> session?.applyUpgrade(action.upgradeId, action.targetPieceId) ?: emptyList()
            is GameAction.SpendMetaCurrency -> {
                profile = MetaProgression.unlock(profile, action.nodeId)
                listOf(GameEvent.SaveRequired)
            }
            GameAction.AbandonRun -> {
                session = null
                modeManager = null
                listOf(GameEvent.SaveRequired, GameEvent.StateChanged)
            }
        }.mapEventsForProfile()
    }

    fun snapshot(): SaveSnapshot = SaveSnapshot(
        activeRun = session?.snapshot(),
        profile = profile,
        activeMods = activeMods,
        contentHash = contentRegistry.contentHash
    )

    private fun List<GameEvent>.mapEventsForProfile(): List<GameEvent> {
        return map { event ->
            when (event) {
                is GameEvent.RunEnded -> {
                    profile = profile.copy(
                        currency = profile.currency + event.awardedCurrency,
                        stats = profile.stats.copy(
                            completedRuns = profile.stats.completedRuns + 1,
                            victories = profile.stats.victories + if (event.victory) 1 else 0,
                            highestRound = maxOf(profile.stats.highestRound, event.finalRound),
                            highScore = maxOf(profile.stats.highScore, event.score)
                        )
                    )
                    session = null
                    modeManager = null
                    event
                }
                else -> event
            }
        }
    }
}
