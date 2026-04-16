package com.chessroguelike.engine

enum class Ability(val displayName: String, val description: String) {
    NONE("없음", "특수 능력 없음"),
    DOUBLE_MOVE("이중 이동", "한 턴에 두 번 이동할 수 있습니다"),
    SHIELD("방어막", "한 번의 공격을 막아냅니다"),
    EXPLOSION("폭발", "기물 포획 시 주변 적 기물도 제거합니다"),
    EXTRA_RANGE("확장 사거리", "추가 이동 범위를 가집니다")
}
