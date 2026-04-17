package com.chessroguelike.app

import android.content.Context
import com.chessroguelike.ai.EnemyAIService
import com.chessroguelike.contentio.ClasspathPackSource
import com.chessroguelike.contentio.DefaultModResolver
import com.chessroguelike.contentio.FileSystemPackSource
import com.chessroguelike.contentio.GameLocalizer
import com.chessroguelike.contentio.ResolvedContentRegistry
import com.chessroguelike.game.ActiveModSnapshot
import com.chessroguelike.game.GameAction
import com.chessroguelike.game.GameEvent
import com.chessroguelike.game.GameRuntime
import com.chessroguelike.game.GameState
import com.chessroguelike.game.ProfileState
import com.chessroguelike.game.SaveSnapshot
import com.chessroguelike.game.SettingsState
import com.chessroguelike.save.JsonSaveRepository
import java.io.File
import java.io.InputStream
import java.util.Locale

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val modsDir = File(appContext.filesDir, "mods")
    private val saveRepository = JsonSaveRepository(File(appContext.filesDir, "save_snapshot.json"))
    private val aiService = EnemyAIService()

    private var saveSnapshot: SaveSnapshot = saveRepository.load() ?: SaveSnapshot(
        profile = ProfileState(
            settings = SettingsState(selectedLocale = initialLocale())
        )
    )
    private var resolvedContent = resolveContent(saveSnapshot.profile.settings.enabledMods.toSet())
    private var localizer = resolvedContent.localizer(saveSnapshot.profile.settings.selectedLocale)
    private var runtime = GameRuntime(resolvedContent, aiService, saveSnapshot.profile, activeMods())
    private var resumeBlocked = false

    init {
        if (saveSnapshot.activeRun != null && isSaveCompatible(saveSnapshot)) {
            runtime.dispatch(GameAction.ResumeRun(saveSnapshot))
        } else if (saveSnapshot.activeRun != null) {
            resumeBlocked = true
        }
    }

    fun state(): GameState? = runtime.currentState()

    fun profile(): ProfileState = runtime.profile

    fun localize(key: String, args: Map<String, String> = emptyMap()): String = localizer.resolve(key, args)

    fun currentLocale(): String = localizer.locale()

    fun supportedLocales(): List<String> = resolvedContent.supportedLocales.sorted()

    fun hasResume(): Boolean = saveSnapshot.activeRun != null && !resumeBlocked

    fun resumeBlockedMessage(): String? = if (resumeBlocked) localize("ui.main.resume_blocked") else null

    fun startNewRun(seed: Long = System.currentTimeMillis()): List<GameEvent> = dispatchAndPersist(GameAction.StartRun(seed))

    fun resumeRun(): List<GameEvent> {
        if (saveSnapshot.activeRun == null || !isSaveCompatible(saveSnapshot)) {
            resumeBlocked = saveSnapshot.activeRun != null
            return emptyList()
        }
        resumeBlocked = false
        return dispatchAndPersist(GameAction.ResumeRun(saveSnapshot))
    }

    fun dispatch(action: GameAction): List<GameEvent> = dispatchAndPersist(action)

    fun setLocale(locale: String) {
        val updatedProfile = runtime.profile.copy(
            settings = runtime.profile.settings.copy(selectedLocale = locale)
        )
        val updatedSave = saveSnapshot.copy(profile = updatedProfile)
        saveSnapshot = updatedSave
        saveRepository.save(updatedSave)
        rebuildRuntime(updatedSave)
    }

    fun importMod(inputStream: InputStream): Result<Unit> = runCatching {
        com.chessroguelike.contentio.ZipModImporter.importZip(inputStream, modsDir)
        val enabledMods = FileSystemPackSource(modsDir, SOURCE_PRIORITY_USER)
            .loadPacks()
            .map { it.manifest.id }
        val updatedProfile = runtime.profile.copy(
            settings = runtime.profile.settings.copy(enabledMods = enabledMods)
        )
        val updatedSave = saveSnapshot.copy(profile = updatedProfile)
        saveSnapshot = updatedSave
        saveRepository.save(updatedSave)
        rebuildRuntime(updatedSave)
    }

    fun clearRun() {
        dispatchAndPersist(GameAction.AbandonRun)
    }

    fun resetToMainMenuSnapshot() {
        saveSnapshot = runtime.snapshot()
        saveRepository.save(saveSnapshot)
    }

    fun upgradeName(upgradeId: String): String =
        localize(resolvedContent.upgradeDefinition(upgradeId).nameKey.value)

    fun upgradeDescription(upgradeId: String): String =
        localize(resolvedContent.upgradeDefinition(upgradeId).descriptionKey.value)

    fun upgradeIcon(upgradeId: String): String = resolvedContent.upgradeDefinition(upgradeId).icon

    fun isAbilityUpgrade(upgradeId: String): Boolean =
        resolvedContent.upgradeDefinition(upgradeId).effect is com.chessroguelike.content.UpgradeEffectDefinition.AddAbility

    fun abilityName(abilityId: String): String =
        localize(resolvedContent.abilityDefinition(abilityId).nameKey.value)

    fun abilityNameForUpgrade(upgradeId: String): String {
        val effect = resolvedContent.upgradeDefinition(upgradeId).effect
        return if (effect is com.chessroguelike.content.UpgradeEffectDefinition.AddAbility) {
            abilityName(effect.abilityId)
        } else {
            ""
        }
    }

    private fun rebuildRuntime(snapshot: SaveSnapshot) {
        resolvedContent = resolveContent(snapshot.profile.settings.enabledMods.toSet())
        localizer = resolvedContent.localizer(snapshot.profile.settings.selectedLocale)
        runtime = GameRuntime(resolvedContent, aiService, snapshot.profile, activeMods())
        resumeBlocked = snapshot.activeRun != null && !isSaveCompatible(snapshot)
        if (!resumeBlocked && snapshot.activeRun != null) {
            runtime.dispatch(GameAction.ResumeRun(snapshot))
        }
    }

    private fun dispatchAndPersist(action: GameAction): List<GameEvent> {
        val events = runtime.dispatch(action)
        if (events.any { it is GameEvent.SaveRequired }) {
            saveSnapshot = runtime.snapshot()
            saveRepository.save(saveSnapshot)
        } else {
            saveSnapshot = saveSnapshot.copy(profile = runtime.profile)
        }
        if (events.any { it is GameEvent.RunEnded }) {
            saveSnapshot = runtime.snapshot()
            saveRepository.save(saveSnapshot)
        }
        return events
    }

    private fun resolveContent(enabledMods: Set<String>): ResolvedContentRegistry {
        val resolver = DefaultModResolver(
            baseSources = listOf(ClasspathPackSource("base-game", SOURCE_PRIORITY_BASE)),
            userSources = listOf(FileSystemPackSource(modsDir, SOURCE_PRIORITY_USER))
        )
        return resolver.resolve(enabledMods)
    }

    private fun activeMods(): List<ActiveModSnapshot> =
        resolvedContent.activeManifests()
            .filterNot { it.id == "base-game" }
            .map { manifest ->
                ActiveModSnapshot(
                    id = manifest.id,
                    version = manifest.version,
                    contentHash = resolvedContent.contentHash
                )
            }

    private fun isSaveCompatible(snapshot: SaveSnapshot): Boolean {
        return snapshot.contentHash == resolvedContent.contentHash &&
            snapshot.activeMods == activeMods()
    }

    private fun initialLocale(): String {
        val language = Locale.getDefault().toLanguageTag()
        return if (language.startsWith("ko")) "ko-KR" else "en"
    }

    companion object {
        private const val SOURCE_PRIORITY_BASE = 0
        private const val SOURCE_PRIORITY_USER = 200
    }
}
