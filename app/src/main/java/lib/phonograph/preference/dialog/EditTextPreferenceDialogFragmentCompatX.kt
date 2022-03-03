package lib.phonograph.preference.dialog

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.preference.EditTextPreference
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import lib.phonograph.preference.EditTextPreferenceX
/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class EditTextPreferenceDialogFragmentCompatX : PreferenceDialogFragmentX() {
    private var input: CharSequence? = null
    private val editTextPreference: EditTextPreferenceX
        get() = preference as EditTextPreferenceX

    @SuppressLint("CheckResult")
    override fun onPrepareDialog(dialog: MaterialDialog) {
        super.onPrepareDialog(dialog)
        val preference: EditTextPreference = editTextPreference
        dialog.input(hint = null, prefill = preference.text, waitForPositiveButton = false) {
            _, text ->
            input = text
        }
        dialog.negativeButton() { dismiss() }
            .positiveButton() {
                preference.text = input as String
                dismiss()
            }
    }

    override fun needInputMethod(): Boolean {
        return true
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
