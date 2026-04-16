package com.chessroguelike.roguelike

import com.chessroguelike.engine.*

class RoundManager {
    var currentRound = 1
        private set
    var score = 0
        private set

    fun addScore(capturedPieces: Int) {
        score += 100 * currentRound + 10 * capturedPieces
    }

    fun nextRound() {
        currentRound++
    }

    fun generateEnemySetup(board: ChessBoard) {
        board.getAllPieces().filter { !it.isPlayer }.forEach { board.removePiece(it) }

        val backRowTypes = listOf(
            PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP, PieceType.QUEEN,
            PieceType.KING, PieceType.BISHOP, PieceType.KNIGHT, PieceType.ROOK
        )
        backRowTypes.forEachIndexed { col, type ->
            board.createAndAddPiece(type, false, 0, col)
        }
        repeat(ChessBoard.SIZE) { col ->
            board.createAndAddPiece(PieceType.PAWN, false, 1, col)
        }

        if (currentRound >= 2) {
            val extraTypes = listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT)
            val extraType = extraTypes[(currentRound - 2) % extraTypes.size]
            val freeCol = findFreeCol(board, 2)
            if (freeCol >= 0) {
                board.createAndAddPiece(extraType, false, 2, freeCol)
            }
        }

        if (currentRound >= 3) {
            val abilities = listOf(Ability.DOUBLE_MOVE, Ability.SHIELD, Ability.EXPLOSION, Ability.EXTRA_RANGE)
            val enemyPieces = board.getEnemyPieces().filter { it.type != PieceType.KING }
            val piecesToUpgrade = minOf(currentRound - 2, enemyPieces.size)
            enemyPieces.shuffled().take(piecesToUpgrade).forEachIndexed { index, piece ->
                piece.ability = abilities[index % abilities.size]
            }
        }
    }

    private fun findFreeCol(board: ChessBoard, row: Int): Int {
        return (0 until ChessBoard.SIZE).firstOrNull { col -> board.getPiece(row, col) == null } ?: -1
    }

    fun generateUpgradeOptions(existingPlayerPieces: List<com.chessroguelike.engine.ChessPiece>): List<Upgrade> {
        val options = mutableListOf<Upgrade>()
        var idCounter = 1

        val addablePieces = listOf(
            PieceType.ROOK to Pair("룩 추가", "♖"),
            PieceType.BISHOP to Pair("비숍 추가", "♗"),
            PieceType.KNIGHT to Pair("나이트 추가", "♘"),
            PieceType.QUEEN to Pair("퀸 추가", "♕")
        )
        val pieceUpgrade = addablePieces.random()
        options.add(
            Upgrade(
                id = idCounter++,
                name = "새 기물 추가: ${pieceUpgrade.first.displayName}",
                description = "플레이어 진영에 ${pieceUpgrade.first.displayName}을(를) 추가합니다",
                upgradeType = UpgradeType.AddPiece(pieceUpgrade.first),
                icon = pieceUpgrade.second.second
            )
        )

        val abilities = listOf(
            Ability.DOUBLE_MOVE to "이중 이동: 한 턴에 두 번 이동",
            Ability.SHIELD to "방어막: 한 번의 공격을 방어",
            Ability.EXPLOSION to "폭발: 포획 시 주변 적 제거",
            Ability.EXTRA_RANGE to "확장 사거리: 이동 범위 증가"
        )
        val abilityUpgrade = abilities.random()
        options.add(
            Upgrade(
                id = idCounter++,
                name = "특수 능력: ${abilityUpgrade.first.displayName}",
                description = "${abilityUpgrade.second}\n기물을 선택하여 능력을 부여합니다",
                upgradeType = UpgradeType.AddAbility(abilityUpgrade.first),
                icon = "✨"
            )
        )

        options.add(
            Upgrade(
                id = idCounter,
                name = "치유",
                description = "잃어버린 플레이어 기물을 복원합니다 (폰 2개 + 랜덤 기물 1개)",
                upgradeType = UpgradeType.Heal,
                icon = "💊"
            )
        )

        return options.shuffled()
    }

    fun applyUpgrade(board: ChessBoard, upgrade: Upgrade, targetPiece: com.chessroguelike.engine.ChessPiece? = null) {
        when (val type = upgrade.upgradeType) {
            is UpgradeType.AddPiece -> {
                val freeSquare = findFreeSquareForPlayer(board)
                if (freeSquare != null) {
                    board.createAndAddPiece(type.pieceType, true, freeSquare.first, freeSquare.second)
                }
            }
            is UpgradeType.AddAbility -> {
                targetPiece?.let { it.ability = type.ability }
            }
            is UpgradeType.Heal -> {
                val missingPawns = 8 - board.getPlayerPieces().count { it.type == PieceType.PAWN }
                val pawnsToAdd = minOf(2, missingPawns)
                repeat(pawnsToAdd) {
                    val sq = findFreeSquareForPlayer(board)
                    if (sq != null) board.createAndAddPiece(PieceType.PAWN, true, sq.first, sq.second)
                }
                val healablePieces = listOf(PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP)
                val healType = healablePieces.random()
                val sq = findFreeSquareForPlayer(board)
                if (sq != null) board.createAndAddPiece(healType, true, sq.first, sq.second)
            }
        }
    }

    private fun findFreeSquareForPlayer(board: ChessBoard): Pair<Int, Int>? {
        for (row in 7 downTo 5) {
            for (col in 0 until ChessBoard.SIZE) {
                if (board.getPiece(row, col) == null) return Pair(row, col)
            }
        }
        return null
    }
}
