/*
 * Copyright (c) 2022 chr_56
 */

package lib.phonograph.localization

import com.google.android.material.dialog.MaterialAlertDialogBuilder
import player.phonograph.R
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.DialogFragment
import android.app.Dialog
import android.os.Bundle
import java.util.*

class LanguageSettingDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val current: Locale = LocalizationStore.current(requireContext())
        var target: Locale = current

        val allNames = getAvailableLanguageNames(current)
        val allLocales = getAvailableLanguage()

        val selected = allLocales.indexOf(current)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.app_language)
            .setSingleChoiceItems(allNames, selected) { _, which ->
                target = allLocales.getOrNull(which) ?: current
            }
            .setPositiveButton(getString(android.R.string.ok)) { dialog, _ ->
                dialog.dismiss()
                AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.create(target)
                )
                LocalizationStore.save(requireContext(), target)
            }
            .setNegativeButton(getString(R.string.reset_action)) { dialog, _ ->
                dialog.dismiss()
                AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.getEmptyLocaleList()
                )
                val locale = AppCompatDelegate.getApplicationLocales()[0] ?: Locale.getDefault()
                LocalizationStore.save(requireContext(), locale)
            }
            .create()
        return dialog
    }

    companion object {
        private const val TAG = "LanguageSettingDialog"
    }
}
