package com.chessroguelike.contentio

import com.chessroguelike.content.AbilityDefinition
import com.chessroguelike.content.BalanceDefinition
import com.chessroguelike.content.ContentPackManifest
import com.chessroguelike.content.ContentRegistry
import com.chessroguelike.content.LocaleCatalog
import com.chessroguelike.content.PieceDefinition
import com.chessroguelike.content.RoundDefinition
import com.chessroguelike.content.UpgradeDefinition
import com.chessroguelike.content.VersionRange
import com.chessroguelike.engine.PieceType
import java.security.MessageDigest
import java.nio.charset.StandardCharsets

class ResolvedContentRegistry(
    override val pieces: Map<PieceType, PieceDefinition>,
    override val abilities: Map<String, AbilityDefinition>,
    override val upgrades: Map<String, UpgradeDefinition>,
    override val rounds: Map<Int, RoundDefinition>,
    override val balance: BalanceDefinition,
    private val localeCatalogs: Map<String, LocaleCatalog>,
    private val manifests: List<ContentPackManifest>,
    override val contentHash: String
) : ContentRegistry {
    override val supportedLocales: Set<String> = localeCatalogs.keys

    fun localizer(locale: String): GameLocalizer = GameLocalizer(locale, localeCatalogs)

    fun activeManifests(): List<ContentPackManifest> = manifests.toList()

    companion object {
        const val GAME_VERSION = "1.0.0"

        fun resolve(
            basePacks: List<ContentPackBundle>,
            officialPacks: List<ContentPackBundle> = emptyList(),
            userPacks: List<ContentPackBundle> = emptyList(),
            enabledModIds: Set<String> = userPacks.map { it.manifest.id }.toSet()
        ): ResolvedContentRegistry {
            val orderedPacks = (basePacks + officialPacks + userPacks.filter { enabledModIds.contains(it.manifest.id) })
                .sortedWith(compareBy<ContentPackBundle> { it.sourcePriority }.thenBy { it.manifest.loadOrderHint }.thenBy { it.manifest.id })

            orderedPacks.forEach { pack ->
                validateVersion(pack.manifest.compatibleGameVersions)
            }
            val manifestIds = orderedPacks.map { it.manifest.id }.toSet()
            orderedPacks.forEach { pack ->
                val missing = pack.manifest.dependencies.filterNot(manifestIds::contains)
                require(missing.isEmpty()) { "Missing dependencies for ${pack.manifest.id}: $missing" }
            }

            val pieces = linkedMapOf<PieceType, PieceDefinition>()
            val abilities = linkedMapOf<String, AbilityDefinition>()
            val upgrades = linkedMapOf<String, UpgradeDefinition>()
            val rounds = linkedMapOf<Int, RoundDefinition>()
            val locales = linkedMapOf<String, MutableMap<String, String>>()
            var balance: BalanceDefinition? = null

            orderedPacks.forEach { pack ->
                pack.content.pieces.forEach { pieces[it.pieceType] = it }
                pack.content.abilities.forEach { abilities[it.id] = it }
                pack.content.upgrades.forEach { upgrades[it.id] = it }
                pack.content.rounds.forEach { rounds[it.round] = it }
                balance = pack.content.balance
                pack.locales.forEach { (locale, catalog) ->
                    val merged = locales.getOrPut(locale) { linkedMapOf() }
                    merged.putAll(catalog.entries)
                }
            }

            val localeCatalogs = locales.mapValues { (locale, entries) -> LocaleCatalog(locale, entries.toMap()) }
            return ResolvedContentRegistry(
                pieces = pieces.toMap(),
                abilities = abilities.toMap(),
                upgrades = upgrades.toMap(),
                rounds = rounds.toMap(),
                balance = requireNotNull(balance) { "No balance definition found" },
                localeCatalogs = localeCatalogs,
                manifests = orderedPacks.map { it.manifest },
                contentHash = hashPayloads(orderedPacks)
            )
        }

        private fun validateVersion(range: VersionRange) {
            require(compareVersions(GAME_VERSION, range.minInclusive) >= 0) {
                "Game version $GAME_VERSION is below minimum ${range.minInclusive}"
            }
            val max = range.maxInclusive
            require(max == null || compareVersions(GAME_VERSION, max) <= 0) {
                "Game version $GAME_VERSION is above maximum $max"
            }
        }

        private fun hashPayloads(packs: List<ContentPackBundle>): String {
            val digest = MessageDigest.getInstance("SHA-256")
            packs.forEach { digest.update(it.rawPayload.toByteArray(StandardCharsets.UTF_8)) }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }

        private fun compareVersions(left: String, right: String): Int {
            val leftParts = left.split(".").map { it.toIntOrNull() ?: 0 }
            val rightParts = right.split(".").map { it.toIntOrNull() ?: 0 }
            val maxSize = maxOf(leftParts.size, rightParts.size)
            for (index in 0 until maxSize) {
                val leftPart = leftParts.getOrElse(index) { 0 }
                val rightPart = rightParts.getOrElse(index) { 0 }
                if (leftPart != rightPart) return leftPart.compareTo(rightPart)
            }
            return 0
        }
    }
}

class GameLocalizer(
    private val selectedLocale: String,
    private val localeCatalogs: Map<String, LocaleCatalog>
) {
    fun resolve(key: String, args: Map<String, String> = emptyMap()): String {
        val primary = localeCatalogs[selectedLocale]
        val fallback = localeCatalogs["en"]
        return (primary ?: fallback ?: error("Missing English locale catalog"))
            .resolve(com.chessroguelike.content.TextKey(key), args, fallback)
    }

    fun locale(): String = selectedLocale
}
