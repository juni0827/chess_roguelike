package com.chessroguelike

import com.chessroguelike.content.AbilityDefinition
import com.chessroguelike.content.AbilityEffectType
import com.chessroguelike.content.BalanceDefinition
import com.chessroguelike.content.ContentRegistry
import com.chessroguelike.content.PieceDefinition
import com.chessroguelike.content.RoundDefinition
import com.chessroguelike.content.TextKey
import com.chessroguelike.content.UpgradeDefinition
import com.chessroguelike.content.UpgradeEffectDefinition
import com.chessroguelike.engine.PieceType

object TestContentRegistry : ContentRegistry {
    override val pieces: Map<PieceType, PieceDefinition> = PieceType.entries.associateWith { type ->
        PieceDefinition("piece.${type.name.lowercase()}", type, TextKey("piece.${type.name.lowercase()}.name"))
    }
    override val abilities: Map<String, AbilityDefinition> = listOf(
        AbilityDefinition("ability.none", AbilityEffectType.NONE, TextKey("ability.none.name"), TextKey("ability.none.description")),
        AbilityDefinition("ability.double_move", AbilityEffectType.DOUBLE_MOVE, TextKey("ability.double_move.name"), TextKey("ability.double_move.description")),
        AbilityDefinition("ability.shield", AbilityEffectType.SHIELD, TextKey("ability.shield.name"), TextKey("ability.shield.description")),
        AbilityDefinition("ability.explosion", AbilityEffectType.EXPLOSION, TextKey("ability.explosion.name"), TextKey("ability.explosion.description")),
        AbilityDefinition("ability.extra_range", AbilityEffectType.EXTRA_RANGE, TextKey("ability.extra_range.name"), TextKey("ability.extra_range.description"))
    ).associateBy { it.id }
    override val upgrades: Map<String, UpgradeDefinition> = listOf(
        UpgradeDefinition(
            id = "upgrade.add_rook",
            nameKey = TextKey("upgrade.add_rook.name"),
            descriptionKey = TextKey("upgrade.add_rook.description"),
            icon = "♖",
            effect = UpgradeEffectDefinition.AddPiece(PieceType.ROOK)
        ),
        UpgradeDefinition(
            id = "upgrade.ability.double_move",
            nameKey = TextKey("upgrade.ability.double_move.name"),
            descriptionKey = TextKey("upgrade.ability.double_move.description"),
            icon = "✨",
            effect = UpgradeEffectDefinition.AddAbility("ability.double_move")
        ),
        UpgradeDefinition(
            id = "upgrade.heal",
            nameKey = TextKey("upgrade.heal.name"),
            descriptionKey = TextKey("upgrade.heal.description"),
            icon = "💊",
            effect = UpgradeEffectDefinition.Heal(2, listOf(PieceType.BISHOP))
        )
    ).associateBy { it.id }
    override val rounds: Map<Int, RoundDefinition> = (1..5).associateWith { round ->
        RoundDefinition(round)
    }
    override val balance: BalanceDefinition = BalanceDefinition(
        maxRounds = 5,
        scoreRoundBase = 100,
        scoreCaptureMultiplier = 10,
        upgradeChoiceCount = 3,
        playerSpawnRows = listOf(7, 6, 5),
        metaCurrencyPerRound = 1,
        metaCurrencyPerVictory = 3
    )
    override val supportedLocales: Set<String> = setOf("ko-KR", "en")
    override val contentHash: String = "test-hash"
}
