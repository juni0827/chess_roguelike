package com.chessroguelike.feature.game

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.chessroguelike.bootstrap.ChessRoguelikeApp
import com.chessroguelike.databinding.ActivityGameBinding
import com.chessroguelike.engine.ChessBoard
import com.chessroguelike.engine.PieceType
import com.chessroguelike.feature.upgrade.UpgradeActivity
import com.chessroguelike.game.GameAction
import com.chessroguelike.game.GameEvent
import com.chessroguelike.game.GameState
import com.chessroguelike.ui.board.BoardView

class GameActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGameBinding
    private val container by lazy { (application as ChessRoguelikeApp).appContainer }
    private var pendingAbilityUpgradeId: String? = null

    companion object {
        private const val STATE_PENDING_ABILITY_UPGRADE_ID = "pending_ability_upgrade_id"
    }

    private val upgradeLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != RESULT_OK) {
            relaunchPendingUpgradePicker()
            return@registerForActivityResult
        }
        val upgradeId = result.data?.getStringExtra(UpgradeActivity.EXTRA_SELECTED_UPGRADE_ID) ?: run {
            relaunchPendingUpgradePicker()
            return@registerForActivityResult
        }
        if (container.isAbilityUpgrade(upgradeId)) {
            pendingAbilityUpgradeId = upgradeId
            val abilityName = container.abilityNameForUpgrade(upgradeId)
            Toast.makeText(
                this,
                container.localize("toast.choose_piece_for_ability", mapOf("ability" to abilityName)),
                Toast.LENGTH_LONG
            ).show()
        } else {
            handleEvents(container.dispatch(GameAction.ChooseUpgrade(upgradeId)))
        }
        renderCurrentState()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)
        pendingAbilityUpgradeId = savedInstanceState?.getString(STATE_PENDING_ABILITY_UPGRADE_ID)

        setupBoardView()
        setupButtons()

        if (containerState() == null) {
            finish()
            return
        }
        renderCurrentState()
        relaunchPendingUpgradePicker()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_PENDING_ABILITY_UPGRADE_ID, pendingAbilityUpgradeId)
    }

    private fun setupBoardView() {
        binding.boardView.onSquareTapped = { tapIntent ->
            when (tapIntent) {
                is BoardView.TapIntent.SelectPiece -> handleSquareSelected(
                    tapIntent.square.row,
                    tapIntent.square.col
                )
                is BoardView.TapIntent.ExecuteMove -> handleSquareSelected(
                    tapIntent.square.row,
                    tapIntent.square.col
                )
            }
        }
    }

    private fun setupButtons() {
        binding.btnSkipDoubleMove.setOnClickListener {
            handleEvents(container.dispatch(GameAction.SkipDoubleMove))
            renderCurrentState()
        }
        binding.btnEndGame.setOnClickListener { showQuitConfirmDialog() }
    }

    private fun handleSquareSelected(row: Int, col: Int) {
        val pendingUpgrade = pendingAbilityUpgradeId
        if (pendingUpgrade != null) {
            val board = containerState()?.let { ChessBoard(it.board) } ?: return
            val piece = board.getPiece(row, col)
            if (piece == null || !piece.isPlayer || piece.type == PieceType.KING) {
                Toast.makeText(this, container.localize("toast.select_player_piece"), Toast.LENGTH_SHORT).show()
                return
            }
            pendingAbilityUpgradeId = null
            handleEvents(container.dispatch(GameAction.ChooseUpgrade(pendingUpgrade, piece.id)))
            renderCurrentState()
            return
        }

        handleEvents(container.dispatch(GameAction.SelectSquare(row, col)))
        renderCurrentState()
    }

    private fun handleEvents(events: List<GameEvent>) {
        events.forEach { event ->
            when (event) {
                is GameEvent.Notification -> {
                    Toast.makeText(
                        this,
                        container.localize(event.message.key.value, event.message.args),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                is GameEvent.MoveExecuted -> binding.boardView.setLastMove(event.move)
                is GameEvent.UpgradeOffered -> launchUpgradePicker(event.upgradeIds)
                is GameEvent.RunEnded -> {
                    if (event.victory) {
                        showVictoryDialog(event.score, event.awardedCurrency)
                    } else {
                        showGameOverDialog(event.finalRound, event.score, event.awardedCurrency)
                    }
                }
                else -> Unit
            }
        }
    }

    private fun launchUpgradePicker(upgradeIds: List<String>) {
        val intent = Intent(this, UpgradeActivity::class.java).apply {
            putStringArrayListExtra(UpgradeActivity.EXTRA_UPGRADE_IDS, ArrayList(upgradeIds))
        }
        upgradeLauncher.launch(intent)
    }

    private fun relaunchPendingUpgradePicker() {
        if (pendingAbilityUpgradeId != null) return
        val upgradeIds = containerState()?.offeredUpgradeIds.orEmpty()
        if (upgradeIds.isNotEmpty()) {
            launchUpgradePicker(upgradeIds)
        }
    }

    private fun renderCurrentState() {
        val state = containerState() ?: return
        val board = ChessBoard(state.board)
        binding.boardView.board = board
        binding.boardView.selectedPiece = state.selectedPieceId?.let(board::getPieceById)
        binding.boardView.validMoves = state.validMoves
        binding.boardView.isInteractionEnabled = shouldEnableBoardInput(state)
        binding.boardView.refresh()

        binding.tvRoundInfo.text = container.localize(
            "ui.game.round_info",
            mapOf("current" to state.currentRound.toString(), "max" to state.maxRounds.toString())
        )
        binding.progressRun.max = state.maxRounds
        binding.progressRun.progress = state.currentRound.coerceIn(1, state.maxRounds)
        binding.tvTurnStatus.text = when {
            pendingAbilityUpgradeId != null -> container.localize("ui.upgrade.title")
            else -> when (state.turnState) {
                com.chessroguelike.engine.TurnState.PLAYER_TURN -> container.localize("ui.game.turn.player")
                com.chessroguelike.engine.TurnState.PLAYER_DOUBLE_MOVE -> container.localize("ui.game.turn.player_double")
                com.chessroguelike.engine.TurnState.ENEMY_TURN -> container.localize("ui.game.turn.enemy")
                com.chessroguelike.engine.TurnState.ROUND_WON -> container.localize("ui.game.turn.round_won")
                com.chessroguelike.engine.TurnState.GAME_OVER -> container.localize("ui.game.turn.game_over")
            }
        }
        binding.tvThreatLevel.text = when {
            state.currentRound <= 1 -> "위협도: 낮음"
            state.currentRound <= 3 -> "위협도: 보통"
            else -> "위협도: 높음"
        }
        binding.tvObjective.text = when {
            pendingAbilityUpgradeId != null -> "목표: 능력을 부여할 기물을 선택"
            state.turnState == com.chessroguelike.engine.TurnState.PLAYER_DOUBLE_MOVE ->
                "목표: 이중 이동 기회를 활용해 핵심 기물을 압박"
            state.turnState == com.chessroguelike.engine.TurnState.ENEMY_TURN ->
                "목표: 상대 반격을 예상하고 킹을 보호"
            else -> "목표: 적 킹을 체크메이트하고 아군 손실 최소화"
        }
        binding.tvScore.text = container.localize("ui.game.score", mapOf("score" to state.score.toString()))
        binding.tvPlayerPieces.text = container.localize(
            "ui.game.player_pieces",
            mapOf("count" to board.getPlayerPieces().size.toString())
        )
        binding.tvCaptured.text = "포획: ${state.capturedByPlayer}"
        binding.tvEnemyPieces.text = container.localize(
            "ui.game.enemy_pieces",
            mapOf("count" to board.getEnemyPieces().size.toString())
        )
        binding.btnSkipDoubleMove.text = container.localize("ui.game.skip_double_move")
        binding.btnEndGame.text = container.localize("ui.game.end_game")
        binding.btnSkipDoubleMove.visibility =
            if (state.turnState == com.chessroguelike.engine.TurnState.PLAYER_DOUBLE_MOVE) View.VISIBLE else View.GONE
    }

    private fun shouldEnableBoardInput(state: GameState): Boolean {
        if (pendingAbilityUpgradeId != null) return true
        return state.turnState == com.chessroguelike.engine.TurnState.PLAYER_TURN ||
            state.turnState == com.chessroguelike.engine.TurnState.PLAYER_DOUBLE_MOVE
    }

    private fun showGameOverDialog(round: Int, score: Int, awardedCurrency: Int) {
        AlertDialog.Builder(this)
            .setTitle(container.localize("dialog.game_over.title"))
            .setMessage(
                container.localize(
                    "dialog.game_over.body",
                    mapOf(
                        "round" to round.toString(),
                        "score" to score.toString(),
                        "currency" to awardedCurrency.toString()
                    )
                )
            )
            .setPositiveButton(container.localize("dialog.common.restart")) { _, _ ->
                pendingAbilityUpgradeId = null
                binding.boardView.setLastMove(null)
                container.startNewRun()
                renderCurrentState()
            }
            .setNegativeButton(container.localize("dialog.common.main_menu")) { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun showVictoryDialog(score: Int, awardedCurrency: Int) {
        AlertDialog.Builder(this)
            .setTitle(container.localize("dialog.victory.title"))
            .setMessage(
                container.localize(
                    "dialog.victory.body",
                    mapOf("score" to score.toString(), "currency" to awardedCurrency.toString())
                )
            )
            .setPositiveButton(container.localize("dialog.common.restart")) { _, _ ->
                pendingAbilityUpgradeId = null
                binding.boardView.setLastMove(null)
                container.startNewRun()
                renderCurrentState()
            }
            .setNegativeButton(container.localize("dialog.common.main_menu")) { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun showQuitConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle(container.localize("dialog.quit.title"))
            .setMessage(container.localize("dialog.quit.body"))
            .setPositiveButton(container.localize("dialog.common.confirm")) { _, _ ->
                container.clearRun()
                finish()
            }
            .setNegativeButton(container.localize("dialog.common.cancel"), null)
            .show()
    }

    private fun containerState(): GameState? = container.state()
}
