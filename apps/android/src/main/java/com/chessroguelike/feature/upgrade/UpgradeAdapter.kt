package com.chessroguelike.feature.upgrade

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.chessroguelike.databinding.ItemUpgradeBinding
import com.chessroguelike.di.AppContainer

class UpgradeAdapter(
    private val upgradeIds: List<String>,
    private val container: AppContainer,
    private val onUpgradeClick: (String) -> Unit
) : RecyclerView.Adapter<UpgradeAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemUpgradeBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemUpgradeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val upgradeId = upgradeIds[position]
        with(holder.binding) {
            tvUpgradeIcon.text = container.upgradeIcon(upgradeId)
            tvUpgradeName.text = container.upgradeName(upgradeId)
            tvUpgradeDescription.text = container.upgradeDescription(upgradeId)
            root.setOnClickListener { onUpgradeClick(upgradeId) }
        }
    }

    override fun getItemCount() = upgradeIds.size
}
