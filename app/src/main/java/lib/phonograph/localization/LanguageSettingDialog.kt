/*
 * Copyright (c) 2022 chr_56
 */

package lib.phonograph.localization

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.*
import player.phonograph.R

class LanguageSettingDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val current: Locale = Localization.storedLocale(requireContext())
        val default: Locale = Localization.defaultLocale()
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
                Localization.setCurrentLocale(
                    context = requireContext(),
                    newLocale = target,
                    recreateActivity = true,
                    saveToPersistence = true
                )
            }
            .setNegativeButton(getString(R.string.reset_action)) { dialog, _ ->
                dialog.dismiss()
                Localization.resetStoredLocale(requireContext())
                Localization.setCurrentLocale(
                    context = requireContext(),
                    newLocale = default,
                    recreateActivity = true,
                    saveToPersistence = false
                )
            }
            .create()
        return dialog
    }

    companion object {
        private const val TAG = "LanguageSettingDialog"
    }
}
