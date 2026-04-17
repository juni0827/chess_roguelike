package com.chessroguelike.game

import kotlinx.serialization.Serializable

@Serializable
data class DeterministicRng(var state: Long) {

    fun nextInt(bound: Int): Int {
        require(bound > 0) { "Bound must be > 0" }
        advance()
        val positive = state ushr 1
        return (positive % bound.toLong()).toInt()
    }

    fun nextBoolean(): Boolean = nextInt(2) == 0

    fun <T> pick(values: List<T>): T {
        require(values.isNotEmpty()) { "Cannot pick from empty list" }
        return values[nextInt(values.size)]
    }

    fun <T> shuffled(values: List<T>): List<T> {
        val list = values.toMutableList()
        for (index in list.lastIndex downTo 1) {
            val swapIndex = nextInt(index + 1)
            val tmp = list[index]
            list[index] = list[swapIndex]
            list[swapIndex] = tmp
        }
        return list
    }

    private fun advance() {
        state = state * 6364136223846793005L + 1442695040888963407L
    }
}
