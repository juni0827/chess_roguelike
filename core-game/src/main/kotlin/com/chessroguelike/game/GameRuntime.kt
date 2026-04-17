package com.chessroguelike.game

import com.chessroguelike.ai.AiService
import com.chessroguelike.content.ContentRegistry

class GameRuntime(
    private val contentRegistry: ContentRegistry,
    private val aiService: AiService,
    profileState: ProfileState = ProfileState(),
    private val activeMods: List<ActiveModSnapshot> = emptyList()
) {
    var profile: ProfileState = profileState
        private set

    private var session: GameSession? = null

    fun currentState(): GameState? = session?.state()

    fun dispatch(action: GameAction): List<GameEvent> {
        return when (action) {
            is GameAction.StartRun -> {
                session = GameSession.new(contentRegistry, aiService, action.seed)
                listOf(GameEvent.StateChanged, GameEvent.SaveRequired)
            }
            is GameAction.ResumeRun -> {
                profile = action.snapshot.profile
                session = action.snapshot.activeRun?.let { GameSession.restore(contentRegistry, aiService, it) }
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
                    event
                }
                else -> event
            }
        }
    }
}
