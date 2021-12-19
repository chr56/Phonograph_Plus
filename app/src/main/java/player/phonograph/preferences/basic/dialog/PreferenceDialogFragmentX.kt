package player.phonograph.preferences.basic.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.preference.DialogPreference
import androidx.preference.DialogPreference.TargetFragment
import androidx.preference.Preference
import chr_56.MDthemer.core.ThemeColor
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
open class PreferenceDialogFragmentX : DialogFragment() {
    private var mWhichButtonClicked: WhichButton? = null
    var preference: DialogPreference? = null
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val rawFragment = this.targetFragment
        check(rawFragment is TargetFragment) { "Target fragment must implement TargetFragment interface" }
        val fragment = rawFragment as TargetFragment
        val key = this.requireArguments().getString(ARG_KEY)
        preference = fragment.findPreference<Preference>(key.toString()) as DialogPreference
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val dialog = MaterialDialog(context)
            .title(text = preference!!.dialogTitle as String)
            .positiveButton(text = preference!!.positiveButtonText)
            .negativeButton(text = preference!!.negativeButtonText)
        //set button color
        dialog.getActionButton(WhichButton.POSITIVE).updateTextColor(ThemeColor.accentColor(requireActivity()))
        dialog.getActionButton(WhichButton.NEGATIVE).updateTextColor(ThemeColor.accentColor(requireActivity()))

        if (needInputMethod()) {
            requestInputMethod(dialog)
        }
        onPrepareDialog(dialog)
        return dialog
    }

    private fun requestInputMethod(dialog: MaterialDialog) {
        val window = dialog.window
        window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
    }
    protected open fun needInputMethod(): Boolean {
        return false
    }

    protected open fun onPrepareDialog(dialog: MaterialDialog) {}

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        mWhichButtonClicked?.let { onDialogClosed(mWhichButtonClicked === WhichButton.POSITIVE) }
    }

    open fun onDialogClosed(positiveResult: Boolean) {}

    companion object {
        @JvmStatic
        protected val ARG_KEY = "key"
        @JvmStatic
        fun newInstance(key: String?): PreferenceDialogFragmentX {
            val fragment = PreferenceDialogFragmentX()
            val b = Bundle(1)
            b.putString(ARG_KEY, key)
            fragment.arguments = b
            return fragment
        }
    }
}
