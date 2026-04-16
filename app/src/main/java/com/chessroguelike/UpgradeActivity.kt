package com.chessroguelike

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chessroguelike.databinding.ActivityUpgradeBinding
import com.chessroguelike.databinding.ItemUpgradeBinding
import com.chessroguelike.engine.Ability
import com.chessroguelike.roguelike.Upgrade
import com.chessroguelike.roguelike.UpgradeType

class UpgradeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUpgradeBinding
    private var selectedUpgrade: Upgrade? = null
    private var targetPieceId: Int = -1

    companion object {
        const val EXTRA_SELECTED_UPGRADE = "selected_upgrade"
        const val EXTRA_TARGET_PIECE_ID = "target_piece_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUpgradeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        @Suppress("DEPRECATION")
        val upgrades = intent.getParcelableArrayListExtra<Upgrade>(GameActivity.EXTRA_UPGRADES) ?: return

        binding.tvUpgradeTitle.text = "업그레이드를 선택하세요"
        binding.tvUpgradeSubtitle.text = "라운드 클리어! 3가지 중 하나를 선택하세요"

        val adapter = UpgradeAdapter(upgrades) { upgrade ->
            handleUpgradeSelected(upgrade)
        }
        binding.rvUpgrades.layoutManager = LinearLayoutManager(this)
        binding.rvUpgrades.adapter = adapter
    }

    private fun handleUpgradeSelected(upgrade: Upgrade) {
        when (upgrade.upgradeType) {
            is UpgradeType.AddAbility -> {
                showPieceSelectionDialog(upgrade)
            }
            else -> {
                confirmAndFinish(upgrade, -1)
            }
        }
    }

    private fun showPieceSelectionDialog(upgrade: Upgrade) {
        val ability = (upgrade.upgradeType as UpgradeType.AddAbility).ability
        Toast.makeText(this, "${ability.displayName} 능력을 부여할 기물을 선택하세요", Toast.LENGTH_LONG).show()
        // For simplicity, auto-select first non-king player piece
        // In a real app, you'd show a board overlay for piece selection
        confirmAndFinish(upgrade, -2)
    }

    private fun confirmAndFinish(upgrade: Upgrade, pieceId: Int) {
        AlertDialog.Builder(this)
            .setTitle("업그레이드 확인")
            .setMessage("'${upgrade.name}'을(를) 선택하시겠습니까?\n\n${upgrade.description}")
            .setPositiveButton("선택") { _, _ ->
                val result = Intent().apply {
                    putExtra(EXTRA_SELECTED_UPGRADE, upgrade)
                    putExtra(EXTRA_TARGET_PIECE_ID, pieceId)
                }
                setResult(Activity.RESULT_OK, result)
                finish()
            }
            .setNegativeButton("취소", null)
            .show()
    }
}

class UpgradeAdapter(
    private val upgrades: List<Upgrade>,
    private val onUpgradeClick: (Upgrade) -> Unit
) : RecyclerView.Adapter<UpgradeAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemUpgradeBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemUpgradeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val upgrade = upgrades[position]
        with(holder.binding) {
            tvUpgradeIcon.text = upgrade.icon
            tvUpgradeName.text = upgrade.name
            tvUpgradeDescription.text = upgrade.description
            root.setOnClickListener { onUpgradeClick(upgrade) }
        }
    }

    override fun getItemCount() = upgrades.size
}
