package com.chessroguelike.game

data class MetaNode(
    val id: String,
    val cost: Int
)

object MetaProgression {
    private val nodes = mapOf(
        "meta.extra_reroll" to MetaNode("meta.extra_reroll", 3),
        "meta.starting_knight" to MetaNode("meta.starting_knight", 5)
    )

    fun unlock(profileState: ProfileState, nodeId: String): ProfileState {
        val node = requireNotNull(nodes[nodeId]) { "Unknown meta node: $nodeId" }
        if (profileState.unlockedNodes.contains(nodeId)) return profileState
        require(profileState.currency >= node.cost) { "Not enough currency for $nodeId" }
        return profileState.copy(
            currency = profileState.currency - node.cost,
            unlockedNodes = profileState.unlockedNodes + nodeId
        )
    }
}
