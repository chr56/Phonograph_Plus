/*
 * Copyright (c) 2022 chr_56
 */

package lib.phonograph.localization

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import player.phonograph.R
import java.util.*

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
            .setPositiveButton(getString(android.R.string.ok)) { _, _ ->
                target?.let {
                    LocalizationUtil.writeLocale(requireContext(), it, true)
                }
            }
            .setNegativeButton(getString(R.string.reset_action)) { _, _ ->
                LocalizationUtil.resetLocale(requireContext(), true)
            }
            .create()
        return dialog
    }

    companion object {
        private const val TAG = "LanguageSettingDialog"
    }
}
