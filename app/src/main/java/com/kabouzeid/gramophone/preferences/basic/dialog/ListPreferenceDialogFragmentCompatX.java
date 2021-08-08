package com.kabouzeid.gramophone.preferences.basic.dialog;

import android.os.Bundle;
import android.view.View;

import androidx.preference.ListPreference;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.kabouzeid.gramophone.preferences.basic.ListPreferenceX;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public class ListPreferenceDialogFragmentCompatX extends PreferenceDialogFragmentX implements MaterialDialog.ListCallbackSingleChoice {
    private int mClickedDialogEntryIndex;

    public static ListPreferenceDialogFragmentCompatX newInstance(String key) {
        final ListPreferenceDialogFragmentCompatX fragment = new ListPreferenceDialogFragmentCompatX();
        final Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);
        return fragment;
    }

    private ListPreferenceX getListPreference() {
        return (ListPreferenceX) getPreference();
    }

    @Override
    protected void onPrepareDialogBuilder(MaterialDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);

        final ListPreference preference = getListPreference();

        if (preference.getEntries() == null || preference.getEntryValues() == null) {
            throw new IllegalStateException(
                    "ListPreference requires an entries array and an entryValues array.");
        }

        mClickedDialogEntryIndex = preference.findIndexOfValue(preference.getValue());
        builder.items(preference.getEntries())
                .alwaysCallSingleChoiceCallback()
                .itemsCallbackSingleChoice(mClickedDialogEntryIndex, this);

        /*
         * The typical interaction for list-based dialogs is to have
         * click-on-an-item dismiss the dialog instead of the user having to
         * press 'Ok'.
         */
        builder.positiveText("");
        builder.negativeText("");
        builder.neutralText("");
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        final ListPreference preference = getListPreference();
        if (positiveResult && mClickedDialogEntryIndex >= 0 &&
                preference.getEntryValues() != null) {
            String value = preference.getEntryValues()[mClickedDialogEntryIndex].toString();
            if (preference.callChangeListener(value)) {
                preference.setValue(value);
            }
        }
    }

    @Override
    public boolean onSelection(MaterialDialog dialog, View itemView, int which, CharSequence text) {
        mClickedDialogEntryIndex = which;
        onClick(dialog, DialogAction.POSITIVE);
        dismiss();
        return true;
    }
}
