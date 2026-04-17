package com.chessroguelike.feature.upgrade

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.chessroguelike.bootstrap.ChessRoguelikeApp
import com.chessroguelike.databinding.ActivityUpgradeBinding

class UpgradeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUpgradeBinding
    private val container by lazy { (application as ChessRoguelikeApp).appContainer }

    companion object {
        const val EXTRA_UPGRADE_IDS = "upgrade_ids"
        const val EXTRA_SELECTED_UPGRADE_ID = "selected_upgrade_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUpgradeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val upgradeIds = intent.getStringArrayListExtra(EXTRA_UPGRADE_IDS)
        if (upgradeIds.isNullOrEmpty()) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }

        binding.tvUpgradeTitle.text = container.localize("ui.upgrade.title")
        binding.tvUpgradeSubtitle.text = container.localize("ui.upgrade.subtitle")
        binding.rvUpgrades.layoutManager = LinearLayoutManager(this)
        binding.rvUpgrades.adapter = UpgradeAdapter(upgradeIds, container) { upgradeId ->
            showConfirmDialog(upgradeId)
        }
    }

    private fun showConfirmDialog(upgradeId: String) {
        val name = container.upgradeName(upgradeId)
        val description = container.upgradeDescription(upgradeId)
        AlertDialog.Builder(this)
            .setTitle(container.localize("dialog.upgrade.confirm.title"))
            .setMessage(
                container.localize(
                    "dialog.upgrade.confirm.body",
                    mapOf("name" to name, "description" to description)
                )
            )
            .setPositiveButton(container.localize("dialog.common.confirm")) { _, _ ->
                setResult(
                    Activity.RESULT_OK,
                    Intent().putExtra(EXTRA_SELECTED_UPGRADE_ID, upgradeId)
                )
                finish()
            }
            .setNegativeButton(container.localize("dialog.common.cancel"), null)
            .show()
    }
}
