/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.setting.dialog

import com.google.android.material.dialog.MaterialAlertDialogBuilder
import player.phonograph.R
import player.phonograph.foundation.localization.ContextLocaleDelegate
import player.phonograph.foundation.localization.LocalizationStore
import player.phonograph.foundation.localization.getAvailableLanguage
import player.phonograph.foundation.localization.getAvailableLanguageNames
import player.phonograph.util.theme.tintButtons
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.DialogFragment
import android.app.Dialog
import android.os.Bundle
import java.util.Locale

class LanguageSettingDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val current: Locale = LocalizationStore.Companion.current(requireContext())
        var target: Locale = current

        val allNames = getAvailableLanguageNames(current)
        val allLocales = getAvailableLanguage()

        val selected = allLocales.indexOf(current)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.label_app_language)
            .setSingleChoiceItems(allNames, selected) { _, which ->
                target = allLocales.getOrNull(which) ?: current
            }
            .setPositiveButton(getString(android.R.string.ok)) { dialog, _ ->
                dialog.dismiss()
                AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.create(target)
                )
                LocalizationStore.Companion.save(requireContext(), target)
            }
            .setNegativeButton(getString(R.string.action_reset)) { dialog, _ ->
                dialog.dismiss()
                AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.getEmptyLocaleList()
                )
                val locale = ContextLocaleDelegate.systemLocale(requireContext())
                LocalizationStore.Companion.save(requireContext(), locale)
            }
            .create()
            .tintButtons()
        return dialog
    }

    companion object {
        private const val TAG = "LanguageSettingDialog"
    }
}