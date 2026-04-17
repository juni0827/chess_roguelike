package com.chessroguelike

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.chessroguelike.databinding.ActivityMainBinding
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val container by lazy { (application as ChessRoguelikeApp).appContainer }

    private val modImportLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@registerForActivityResult
        contentResolver.openInputStream(uri)?.use { input ->
            val result = container.importMod(input)
            if (result.isSuccess) {
                Toast.makeText(this, container.localize("toast.mod_import_success"), Toast.LENGTH_SHORT).show()
                render()
            } else {
                Toast.makeText(this, container.localize("toast.mod_import_failure"), Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnStartGame.setOnClickListener {
            container.startNewRun()
            startActivity(Intent(this, GameActivity::class.java))
        }

        binding.btnContinueGame.setOnClickListener {
            val resumeEvents = container.resumeRun()
            if (resumeEvents.isEmpty() && !container.hasResume()) {
                container.resumeBlockedMessage()?.let { message ->
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                }
                return@setOnClickListener
            }
            startActivity(Intent(this, GameActivity::class.java))
        }

        binding.btnImportMod.setOnClickListener {
            modImportLauncher.launch(arrayOf("application/zip", "application/octet-stream"))
        }

        binding.btnLanguage.setOnClickListener { showLanguageDialog() }
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        binding.tvGameTitle.text = container.localize("ui.main.title")
        binding.tvSubtitle.text = container.localize("ui.main.subtitle")
        binding.btnStartGame.text = container.localize("ui.main.start")
        binding.btnContinueGame.text = container.localize("ui.main.continue")
        binding.btnImportMod.text = container.localize("ui.main.import_mod")
        binding.btnLanguage.text = container.localize("ui.main.language")
        binding.tvRulesTitle.text = container.localize("ui.main.rules_title")
        binding.tvRulesContent.text = container.localize("ui.main.rules_body")
        binding.tvProfileSummary.text = container.localize(
            "ui.main.profile_summary",
            mapOf(
                "currency" to container.profile().currency.toString(),
                "highScore" to container.profile().stats.highScore.toString()
            )
        )
        binding.tvModSummary.text = container.localize(
            "ui.main.mod_summary",
            mapOf("count" to container.profile().settings.enabledMods.size.toString())
        )
        binding.tvLocaleSummary.text = container.localize(
            "ui.main.locale_summary",
            mapOf("locale" to localeLabel(container.currentLocale()))
        )
        binding.tvRunSummary.text = when {
            container.resumeBlockedMessage() != null -> container.resumeBlockedMessage()
            container.hasResume() -> container.localize("ui.main.resume_ready")
            else -> container.localize("ui.main.no_save")
        }
        binding.btnContinueGame.isEnabled = container.hasResume()
        binding.btnContinueGame.alpha = if (container.hasResume()) 1f else 0.55f
    }

    private fun showLanguageDialog() {
        val supportedLocales = container.supportedLocales()
        val labels = supportedLocales.map(::localeLabel).toTypedArray()
        val checkedIndex = supportedLocales.indexOf(container.currentLocale()).coerceAtLeast(0)
        AlertDialog.Builder(this)
            .setTitle(container.localize("dialog.language.title"))
            .setSingleChoiceItems(labels, checkedIndex) { dialog, which ->
                container.setLocale(supportedLocales[which])
                render()
                dialog.dismiss()
            }
            .setNegativeButton(container.localize("dialog.common.cancel"), null)
            .show()
    }

    private fun localeLabel(tag: String): String {
        val locale = Locale.forLanguageTag(tag)
        return locale.getDisplayName(locale).replaceFirstChar { ch -> ch.titlecase(locale) }
    }
}
