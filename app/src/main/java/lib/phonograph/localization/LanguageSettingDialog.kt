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
import androidx.core.os.LocaleListCompat

class LanguageSettingDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val current: Locale = Localization.storedLocale(requireContext())
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
                Localization.saveLocale(requireContext(), target)
                Localization.modifyLocale(
                    context = requireContext(),
                    newLocales = LocaleListCompat.create(target)
                )
            }
            .setNegativeButton(getString(R.string.reset_action)) { dialog, _ ->
                dialog.dismiss()
                Localization.resetStoredLocale(requireContext())
                Localization.modifyLocale(
                    context = requireContext(),
                    newLocales = LocaleListCompat.getEmptyLocaleList()
                )
            }
            .create()
        return dialog
    }

    companion object {
        private const val TAG = "LanguageSettingDialog"
    }
}
