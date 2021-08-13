package com.kabouzeid.gramophone.preferences.basic.dialog;

import android.os.Bundle;

import androidx.annotation.NonNull;

import com.afollestad.materialdialogs.MaterialDialog;
import com.kabouzeid.gramophone.preferences.basic.EditTextPreferenceX;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public class EditTextPreferenceDialogFragmentCompatX extends PreferenceDialogFragmentX implements MaterialDialog.InputCallback {

    private CharSequence input;

    public static EditTextPreferenceDialogFragmentCompatX newInstance(String key) {
        EditTextPreferenceDialogFragmentCompatX fragment = new EditTextPreferenceDialogFragmentCompatX();
        Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);
        return fragment;
    }

    private EditTextPreferenceX getEditTextPreference() {
        return (EditTextPreferenceX) getPreference();
    }

    @Override
    protected void onPrepareDialogBuilder(MaterialDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);
        builder.input("", getEditTextPreference().getText(), this);
    }

    protected boolean needInputMethod() {
        return true;
    }

    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            String value = input.toString();
            if (this.getEditTextPreference().callChangeListener(value)) {
                this.getEditTextPreference().setText(value);
            }
        }

    }

    @Override
    public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
        this.input = input;
    }
}
