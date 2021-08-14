package com.kabouzeid.gramophone.preferences.basic.dialog

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.preference.ListPreference
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.internal.button.DialogActionButton
import com.afollestad.materialdialogs.list.listItems
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.google.android.material.dialog.MaterialDialogs
import com.kabouzeid.gramophone.preferences.basic.ListPreferenceX

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class ListPreferenceDialogFragmentCompatX : PreferenceDialogFragmentX() {
    private var mClickedDialogEntryIndex = 0
    private val listPreference: ListPreferenceX
        get() = preference as ListPreferenceX

    @SuppressLint("CheckResult")
    fun onPrepareDialogBuilder(dialog: MaterialDialog) {
        super.onPrepareDialog(dialog)
        val preference: ListPreference = listPreference
        check(!(preference.entries == null || preference.entryValues == null)) { "ListPreference requires an entries array and an entryValues array." }
        mClickedDialogEntryIndex = preference.findIndexOfValue(preference.value)
        dialog.listItemsSingleChoice(items = preference.entries as List<CharSequence>)
//        {
//        }
//                .itemsCallbackSingleChoice(mClickedDialogEntryIndex, this)
// TODO

        /*
         * The typical interaction for list-based dialogs is to have
         * click-on-an-item dismiss the dialog instead of the user having to
         * press 'Ok'.
         */
        dialog.positiveButton(text = null)
        dialog.negativeButton(text = null)
        dialog.negativeButton(text = null)
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        val preference: ListPreference = listPreference
        if (positiveResult && mClickedDialogEntryIndex >= 0 && preference.entryValues != null) {
            val value = preference.entryValues[mClickedDialogEntryIndex].toString()
            if (preference.callChangeListener(value)) {
                preference.value = value
            }
        }
    }

    fun onSelection(dialog: MaterialDialog?, itemView: View?, which: Int, text: CharSequence?): Boolean {
        mClickedDialogEntryIndex = which
        onClick(dialog!!, WhichButton.POSITIVE)
        dismiss()
        return true
    }

    companion object {
        @JvmStatic
        fun newInstance(key: String?): ListPreferenceDialogFragmentCompatX {
            val fragment = ListPreferenceDialogFragmentCompatX()
            val b = Bundle(1)
            b.putString(ARG_KEY, key)
            fragment.arguments = b
            return fragment
        }
    }
}