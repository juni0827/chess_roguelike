package com.chessroguelike.ai

import com.chessroguelike.content.ContentRegistry
import com.chessroguelike.engine.ChessBoard
import com.chessroguelike.engine.Move
import com.chessroguelike.game.DeterministicRng

interface AiService {
    fun getBestMove(
        board: ChessBoard,
        contentRegistry: ContentRegistry,
        round: Int,
        rng: DeterministicRng
    ): Move?
}
