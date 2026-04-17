package com.chessroguelike.contentio

import com.chessroguelike.content.AbilityDefinition
import com.chessroguelike.content.BalanceDefinition
import com.chessroguelike.content.PieceDefinition
import com.chessroguelike.content.RoundDefinition
import com.chessroguelike.content.UpgradeDefinition
import kotlinx.serialization.Serializable

@Serializable
data class GameContentFile(
    val pieces: List<PieceDefinition>,
    val abilities: List<AbilityDefinition>,
    val upgrades: List<UpgradeDefinition>,
    val rounds: List<RoundDefinition>,
    val balance: BalanceDefinition
)
