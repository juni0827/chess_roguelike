package com.chessroguelike.contentio

interface ModResolver {
    fun resolve(enabledModIds: Set<String>): ResolvedContentRegistry
}

class DefaultModResolver(
    private val baseSources: List<ContentPackSource>,
    private val officialSources: List<ContentPackSource> = emptyList(),
    private val userSources: List<ContentPackSource> = emptyList()
) : ModResolver {
    override fun resolve(enabledModIds: Set<String>): ResolvedContentRegistry {
        val json = JsonSupport.json
        val basePacks = baseSources.flatMap { it.loadPacks(json) }
        val officialPacks = officialSources.flatMap { it.loadPacks(json) }
        val userPacks = userSources.flatMap { it.loadPacks(json) }
        return ResolvedContentRegistry.resolve(basePacks, officialPacks, userPacks, enabledModIds)
    }
}
