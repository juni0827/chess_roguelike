package com.chessroguelike.content

import com.chessroguelike.engine.PieceType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DefinitionId(val value: String)

@Serializable
data class TextKey(val value: String)

@Serializable
data class LocalizedText(
    val key: TextKey,
    val args: Map<String, String> = emptyMap()
)

@Serializable
data class AssetRef(val path: String)

@Serializable
data class LocaleCatalog(
    val locale: String,
    val entries: Map<String, String>
) {
    fun resolve(
        key: TextKey,
        args: Map<String, String> = emptyMap(),
        fallback: LocaleCatalog? = null
    ): String {
        val template = entries[key.value] ?: fallback?.entries?.get(key.value) ?: key.value
        return args.entries.fold(template) { acc, (name, value) ->
            acc.replace("{$name}", value)
        }
    }
}

@Serializable
data class VersionRange(
    val minInclusive: String,
    val maxInclusive: String? = null
)

@Serializable
data class ContentPackManifest(
    val id: String,
    val version: String,
    val compatibleGameVersions: VersionRange,
    val dependencies: List<String>,
    val loadOrderHint: Int,
    val supportedLocales: List<String>
)

@Serializable
data class PieceDefinition(
    val id: String,
    val pieceType: PieceType,
    val nameKey: TextKey,
    val icon: String? = null,
    val assetRef: AssetRef? = null
)

@Serializable
enum class AbilityEffectType {
    NONE,
    DOUBLE_MOVE,
    SHIELD,
    EXPLOSION,
    EXTRA_RANGE
}

@Serializable
data class AbilityDefinition(
    val id: String,
    val effectType: AbilityEffectType,
    val nameKey: TextKey,
    val descriptionKey: TextKey,
    val icon: String? = null
)

@Serializable
sealed class UpgradeEffectDefinition {
    @Serializable
    @SerialName("add_piece")
    data class AddPiece(val pieceType: PieceType) : UpgradeEffectDefinition()

    @Serializable
    @SerialName("add_ability")
    data class AddAbility(val abilityId: String) : UpgradeEffectDefinition()

    @Serializable
    @SerialName("heal")
    data class Heal(
        val pawnCount: Int,
        val extraPiecePool: List<PieceType>
    ) : UpgradeEffectDefinition()
}

@Serializable
data class UpgradeDefinition(
    val id: String,
    val nameKey: TextKey,
    val descriptionKey: TextKey,
    val icon: String,
    val effect: UpgradeEffectDefinition,
    val weight: Int = 1
)

@Serializable
data class RoundDefinition(
    val round: Int,
    val bonusEnemyPieceType: PieceType? = null,
    val enemyAbilityIds: List<String> = emptyList()
)

@Serializable
data class BalanceDefinition(
    val maxRounds: Int,
    val scoreRoundBase: Int,
    val scoreCaptureMultiplier: Int,
    val upgradeChoiceCount: Int,
    val playerSpawnRows: List<Int>,
    val metaCurrencyPerRound: Int,
    val metaCurrencyPerVictory: Int
)

interface ContentRegistry {
    val pieces: Map<PieceType, PieceDefinition>
    val abilities: Map<String, AbilityDefinition>
    val upgrades: Map<String, UpgradeDefinition>
    val rounds: Map<Int, RoundDefinition>
    val balance: BalanceDefinition
    val supportedLocales: Set<String>
    val contentHash: String

    fun pieceNameKey(pieceType: PieceType): TextKey = requireNotNull(pieces[pieceType]) {
        "Missing piece definition for $pieceType"
    }.nameKey

    fun abilityDefinition(abilityId: String?): AbilityDefinition {
        val resolvedId = abilityId ?: BUILTIN_ABILITY_NONE
        return requireNotNull(abilities[resolvedId]) { "Missing ability definition for $resolvedId" }
    }

    fun upgradeDefinition(upgradeId: String): UpgradeDefinition =
        requireNotNull(upgrades[upgradeId]) { "Missing upgrade definition for $upgradeId" }

    fun roundDefinition(round: Int): RoundDefinition =
        requireNotNull(rounds[round]) { "Missing round definition for round=$round" }

    companion object {
        const val BUILTIN_ABILITY_NONE = "ability.none"
    }
}
