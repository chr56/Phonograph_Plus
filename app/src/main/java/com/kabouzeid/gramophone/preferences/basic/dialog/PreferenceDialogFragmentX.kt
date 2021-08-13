package com.kabouzeid.gramophone.preferences.basic.dialog

import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.preference.DialogPreference
import androidx.preference.DialogPreference.TargetFragment
import androidx.preference.Preference
import com.afollestad.materialdialogs.DialogCallback
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
open class PreferenceDialogFragmentX : DialogFragment() {
    var preference: DialogPreference? = null
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val rawFragment = this.targetFragment
        check(rawFragment is TargetFragment) { "Target fragment must implement TargetFragment interface" }
        val fragment = rawFragment as TargetFragment
        val key = this.requireArguments().getString(ARG_KEY)
        preference = key?.let { fragment.findPreference<Preference>(it) } as DialogPreference
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = this.activity
        val dialog = MaterialDialog(context as Activity)
            .title(text = preference!!.dialogTitle as String)
            .icon(drawable = preference!!.dialogIcon)
            .message(text = preference!!.dialogMessage)
            .positiveButton(text = preference!!.positiveButtonText, click = PositiveListener())
            .negativeButton(text = preference!!.negativeButtonText, click = NegativeListener())
        if (needInputMethod()) {
            requestInputMethod(dialog)
        }
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

    fun onClick(dialog: MaterialDialog, which: WhichButton) {
        mWhichButtonClicked = which
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDialogClosed(mWhichButtonClicked === WhichButton.POSITIVE)
    }

    open fun onDialogClosed(positiveResult: Boolean) {}

    internal open class PositiveListener : DialogCallback {
        override fun invoke(d: MaterialDialog) {
            positiveClick(d)
        }
        open fun positiveClick(d: MaterialDialog) {
        }
    }

    internal open class NegativeListener : DialogCallback {
        override fun invoke(d: MaterialDialog) {
            negativeClick(d)
        }
        open fun negativeClick(d: MaterialDialog) {
        }
    }
//    open fun positiveClick(d: MaterialDialog) {}
//    open fun negativeClick(d: MaterialDialog) {}

    companion object {
        private lateinit var mWhichButtonClicked: WhichButton
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
