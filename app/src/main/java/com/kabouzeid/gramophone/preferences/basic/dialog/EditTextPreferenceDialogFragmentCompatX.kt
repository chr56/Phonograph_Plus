package com.kabouzeid.gramophone.preferences.basic.dialog

import android.os.Bundle
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.InputCallback
import com.afollestad.materialdialogs.input.input
import com.kabouzeid.gramophone.preferences.basic.EditTextPreferenceX

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class EditTextPreferenceDialogFragmentCompatX : PreferenceDialogFragmentX(), InputCallback {
    private var input: CharSequence? = null
    private val editTextPreference: EditTextPreferenceX
        get() = preference as EditTextPreferenceX

    override fun onPrepareDialog(dialog: MaterialDialog) {
        super.onPrepareDialog(dialog)
        dialog.input(hint = null, prefill = editTextPreference.text, callback = this)
    }

    override fun needInputMethod(): Boolean {
        return true
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            val value = input.toString()
            if (editTextPreference.callChangeListener(value)) {
                editTextPreference.text = value
            }
        }
    }

    fun onInput(dialog: MaterialDialog, input: CharSequence?) {
        this.input = input
    }
    override fun invoke(d: MaterialDialog, s: CharSequence) {
    }
    companion object {
        @JvmStatic
        fun newInstance(key: String?): EditTextPreferenceDialogFragmentCompatX {
            val fragment = EditTextPreferenceDialogFragmentCompatX()
            val b = Bundle(1)
            b.putString(ARG_KEY, key)
            fragment.arguments = b
            return fragment
        }
    }
}
