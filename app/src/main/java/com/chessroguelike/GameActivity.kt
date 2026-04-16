package com.chessroguelike

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.chessroguelike.ai.EnemyAI
import com.chessroguelike.databinding.ActivityGameBinding
import com.chessroguelike.engine.*
import com.chessroguelike.roguelike.RoundManager
import com.chessroguelike.roguelike.Upgrade

class GameActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGameBinding
    private val gameEngine = GameEngine()
    private val roundManager = RoundManager()
    private val handler = Handler(Looper.getMainLooper())
    private var enemyAI = EnemyAI(1)

    companion object {
        const val EXTRA_UPGRADES = "upgrades"
        const val REQUEST_UPGRADE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        gameEngine.initGame()
        setupBoardView()
        setupButtons()
        updateStatusUI()
    }

    private fun setupBoardView() {
        binding.boardView.board = gameEngine.board
        binding.boardView.onPieceSelected = { row, col ->
            handlePieceSelection(row, col)
        }
        binding.boardView.onMoveSelected = { row, col ->
            handleMoveSelection(row, col)
        }
    }

    private fun setupButtons() {
        binding.btnSkipDoubleMove.setOnClickListener {
            gameEngine.skipDoubleMove()
            binding.btnSkipDoubleMove.visibility = View.GONE
            updateBoardView()
            updateStatusUI()
            triggerEnemyMove()
        }

        binding.btnEndGame.setOnClickListener {
            showQuitConfirmDialog()
        }
    }

    private fun handlePieceSelection(row: Int, col: Int) {
        if (gameEngine.turnState != TurnState.PLAYER_TURN &&
            gameEngine.turnState != TurnState.PLAYER_DOUBLE_MOVE) return

        val selected = gameEngine.selectPiece(row, col)
        updateBoardView()
        if (!selected) {
            val piece = gameEngine.board.getPiece(row, col)
            if (piece != null && !piece.isPlayer) {
                Toast.makeText(this, "적의 기물입니다", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleMoveSelection(row: Int, col: Int) {
        if (gameEngine.turnState != TurnState.PLAYER_TURN &&
            gameEngine.turnState != TurnState.PLAYER_DOUBLE_MOVE) return

        val fromRow = gameEngine.selectedPiece?.row ?: return
        val fromCol = gameEngine.selectedPiece?.col ?: return

        val result = gameEngine.makePlayerMove(row, col)
        val move = Move(fromRow, fromCol, row, col)
        binding.boardView.setLastMove(move)

        when (result) {
            MoveResult.MOVE_OK -> {
                updateBoardView()
                updateStatusUI()
                binding.btnSkipDoubleMove.visibility = View.GONE
                triggerEnemyMove()
            }
            MoveResult.DOUBLE_MOVE_AVAILABLE -> {
                updateBoardView()
                updateStatusUI()
                binding.btnSkipDoubleMove.visibility = View.VISIBLE
                Toast.makeText(this, "이중 이동! 한 번 더 이동하거나 건너뛰세요", Toast.LENGTH_SHORT).show()
            }
            MoveResult.ROUND_WON -> {
                updateBoardView()
                updateStatusUI()
                binding.btnSkipDoubleMove.visibility = View.GONE
                handler.postDelayed({ handleRoundWon() }, 600)
            }
            MoveResult.GAME_OVER -> {
                updateBoardView()
                handleGameOver()
            }
            MoveResult.INVALID -> {
                updateBoardView()
            }
        }
    }

    private fun triggerEnemyMove() {
        binding.boardView.isInteractionEnabled = false
        handler.postDelayed({
            performEnemyMove()
        }, 700)
    }

    private fun performEnemyMove() {
        enemyAI = EnemyAI(roundManager.currentRound)
        val move = enemyAI.getBestMove(gameEngine.board)
        if (move == null) {
            // Enemy has no moves - treat as round won
            handleRoundWon()
            return
        }

        binding.boardView.setLastMove(move)
        val result = gameEngine.makeEnemyMove(move)
        updateBoardView()
        updateStatusUI()

        when (result) {
            MoveResult.GAME_OVER -> {
                handleGameOver()
            }
            else -> {
                binding.boardView.isInteractionEnabled = true
            }
        }
    }

    private fun handleRoundWon() {
        val capturedCount = gameEngine.capturedByPlayer
        roundManager.addScore(capturedCount)

        val round = roundManager.currentRound
        if (round >= 5) {
            showVictoryDialog()
            return
        }

        val upgrades = roundManager.generateUpgradeOptions(gameEngine.board.getPlayerPieces())
        val intent = Intent(this, UpgradeActivity::class.java).apply {
            putParcelableArrayListExtra(EXTRA_UPGRADES, ArrayList(upgrades))
        }
        @Suppress("DEPRECATION")
        startActivityForResult(intent, REQUEST_UPGRADE)
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_UPGRADE && resultCode == RESULT_OK) {
            val upgrade = data?.getParcelableExtra<Upgrade>(UpgradeActivity.EXTRA_SELECTED_UPGRADE)
            val targetPieceId = data?.getIntExtra(UpgradeActivity.EXTRA_TARGET_PIECE_ID, -1) ?: -1

            if (upgrade != null) {
                val targetPiece = if (targetPieceId >= 0)
                    gameEngine.board.getPlayerPieces().find { it.id == targetPieceId }
                else null
                roundManager.applyUpgrade(gameEngine.board, upgrade, targetPiece)
            }

            roundManager.nextRound()
            roundManager.generateEnemySetup(gameEngine.board)
            gameEngine.startNextRound()
            enemyAI = EnemyAI(roundManager.currentRound)
            binding.boardView.setLastMove(null)
            updateBoardView()
            updateStatusUI()
        }
    }

    private fun handleGameOver() {
        binding.boardView.isInteractionEnabled = false
        AlertDialog.Builder(this)
            .setTitle("게임 오버")
            .setMessage("킹이 잡혔습니다!\n\n라운드: ${roundManager.currentRound}\n점수: ${roundManager.score}")
            .setPositiveButton("다시 시작") { _, _ ->
                gameEngine.initGame()
                roundManager.apply {
                    // Reset round manager
                }
                recreate()
            }
            .setNegativeButton("메인 메뉴") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun showVictoryDialog() {
        binding.boardView.isInteractionEnabled = false
        AlertDialog.Builder(this)
            .setTitle("🏆 승리!")
            .setMessage("5 라운드를 모두 클리어했습니다!\n\n최종 점수: ${roundManager.score}\n포획한 기물: ${gameEngine.capturedByPlayer}")
            .setPositiveButton("다시 시작") { _, _ ->
                recreate()
            }
            .setNegativeButton("메인 메뉴") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun showQuitConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle("게임 종료")
            .setMessage("게임을 종료하시겠습니까?")
            .setPositiveButton("종료") { _, _ -> finish() }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun updateBoardView() {
        binding.boardView.selectedPiece = gameEngine.selectedPiece
        binding.boardView.validMoves = gameEngine.validMoves
        binding.boardView.refresh()
    }

    private fun updateStatusUI() {
        val turnText = when (gameEngine.turnState) {
            TurnState.PLAYER_TURN -> "플레이어 턴"
            TurnState.PLAYER_DOUBLE_MOVE -> "이중 이동 - 한 번 더!"
            TurnState.ENEMY_TURN -> "적 턴..."
            TurnState.ROUND_WON -> "라운드 승리!"
            TurnState.GAME_OVER -> "게임 오버"
        }
        binding.tvTurnStatus.text = turnText
        binding.tvRoundInfo.text = "라운드 ${roundManager.currentRound}/5"
        binding.tvScore.text = "점수: ${roundManager.score}"

        val playerPieces = gameEngine.board.getPlayerPieces()
        val enemyPieces = gameEngine.board.getEnemyPieces()
        binding.tvPlayerPieces.text = "내 기물: ${playerPieces.size}개"
        binding.tvEnemyPieces.text = "적 기물: ${enemyPieces.size}개"
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
