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

    private val _names = getAvailableLanguageNames(LocalizationUtil.locale)
    private val _locales = getAvailableLanguage()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        var target: Locale? = null

        val names = arrayOf(getString(R.string._default)).plus(_names)
        val locales = arrayOf(LocalizationUtil.systemLocale).plus(_locales)

        val selected = locales.indexOf(LocalizationUtil.locale)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.app_language)
            .setSingleChoiceItems(names, selected) { _, which ->
                target = locales.getOrNull(which)
            }
            .setPositiveButton(getString(android.R.string.ok)) { dialog, _ ->
                target?.let {
                    LocalizationUtil.writeLocale(requireContext(), it)
                    LocalizationUtil.setCurrentLocale(requireContext(), it, true)
                }
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.reset_action)) { dialog, _ ->
                LocalizationUtil.resetLocale(requireContext())
                LocalizationUtil.setCurrentLocale(
                    requireContext(),
                    LocalizationUtil.systemLocale,
                    true
                )
                dialog.dismiss()
            }
            .create()
        return dialog
    }

    companion object {
        private const val TAG = "LanguageSettingDialog"
    }
}
