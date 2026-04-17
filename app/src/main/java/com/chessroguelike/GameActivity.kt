package com.chessroguelike

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.chessroguelike.databinding.ActivityGameBinding
import com.chessroguelike.engine.ChessBoard
import com.chessroguelike.engine.PieceType
import com.chessroguelike.game.GameAction
import com.chessroguelike.game.GameEvent
import com.chessroguelike.game.GameState

class GameActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGameBinding
    private val container by lazy { (application as ChessRoguelikeApp).appContainer }
    private var pendingAbilityUpgradeId: String? = null

    private val upgradeLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != RESULT_OK) return@registerForActivityResult
        val upgradeId = result.data?.getStringExtra(UpgradeActivity.EXTRA_SELECTED_UPGRADE_ID) ?: return@registerForActivityResult
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

        setupBoardView()
        setupButtons()

        if (containerState() == null) {
            finish()
            return
        }
        renderCurrentState()
    }

    private fun setupBoardView() {
        binding.boardView.onPieceSelected = { row, col -> handleSquareSelected(row, col) }
        binding.boardView.onMoveSelected = { row, col -> handleSquareSelected(row, col) }
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

    private fun renderCurrentState() {
        val state = containerState() ?: return
        val board = ChessBoard(state.board)
        binding.boardView.board = board
        binding.boardView.selectedPiece = state.selectedPieceId?.let(board::getPieceById)
        binding.boardView.validMoves = state.validMoves
        binding.boardView.isInteractionEnabled = true
        binding.boardView.refresh()

        binding.tvRoundInfo.text = container.localize(
            "ui.game.round_info",
            mapOf("current" to state.currentRound.toString(), "max" to state.maxRounds.toString())
        )
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
        binding.tvScore.text = container.localize("ui.game.score", mapOf("score" to state.score.toString()))
        binding.tvPlayerPieces.text = container.localize(
            "ui.game.player_pieces",
            mapOf("count" to board.getPlayerPieces().size.toString())
        )
        binding.tvEnemyPieces.text = container.localize(
            "ui.game.enemy_pieces",
            mapOf("count" to board.getEnemyPieces().size.toString())
        )
        binding.btnSkipDoubleMove.text = container.localize("ui.game.skip_double_move")
        binding.btnEndGame.text = container.localize("ui.game.end_game")
        binding.btnSkipDoubleMove.visibility =
            if (state.turnState == com.chessroguelike.engine.TurnState.PLAYER_DOUBLE_MOVE) View.VISIBLE else View.GONE
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
