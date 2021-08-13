package com.kabouzeid.gramophone.preferences.basic.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.DialogPreference;

//import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.dialog.MaterialDialogs;

import static android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public class PreferenceDialogFragmentX extends DialogFragment /*implements MaterialDialog.SingleButtonCallback*/ {
    //private DialogAction mWhichButtonClicked;

    protected static final String ARG_KEY = "key";
    private DialogPreference mPreference;

    public static PreferenceDialogFragmentX newInstance(String key) {
        PreferenceDialogFragmentX fragment = new PreferenceDialogFragmentX();
        Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);
        return fragment;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fragment rawFragment = this.getTargetFragment();
        if (!(rawFragment instanceof DialogPreference.TargetFragment)) {
            throw new IllegalStateException("Target fragment must implement TargetFragment interface");
        } else {
            DialogPreference.TargetFragment fragment = (DialogPreference.TargetFragment) rawFragment;
            String key = this.getArguments().getString(ARG_KEY);
            this.mPreference = (DialogPreference) fragment.findPreference(key);
        }
    }

    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        FragmentActivity context = this.getActivity();
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setTitle(this.mPreference.getDialogTitle())
                .setIcon(this.mPreference.getDialogIcon())
//                .onAny(this)
                .setPositiveButton(this.mPreference.getPositiveButtonText(),new PositiveListener())
                .setNegativeButton(this.mPreference.getNegativeButtonText(),new NegativeListener())
                .setMessage(this.mPreference.getDialogMessage());
        this.onPrepareDialogBuilder(builder);
        AlertDialog dialog = builder.create();
        if (this.needInputMethod()) {
            this.requestInputMethod(dialog);
        }

        return dialog;
    }

    public DialogPreference getPreference() {
        return this.mPreference;
    }

    protected void onPrepareDialogBuilder(MaterialAlertDialogBuilder builder) {
    }

    protected boolean needInputMethod() {
        return false;
    }

    private void requestInputMethod(Dialog dialog) {
        Window window = dialog.getWindow();
        window.setSoftInputMode(SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }

//    @Override
//    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
//        mWhichButtonClicked = which;
//    }

//    @Override
//    public void onDismiss(DialogInterface dialog) {
//        super.onDismiss(dialog);
//        onDialogClosed(mWhichButtonClicked == DialogAction.POSITIVE);
//    }

//    public void onDialogClosed(boolean positiveResult) {
//
//    }

    static class PositiveListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            PreferenceDialogFragmentX.positiveClick(v);
        }
    }
    private static void positiveClick(View v) {
    }

    static class NegativeListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            PreferenceDialogFragmentX.negativeClick(v);
        }
    }
    private static void negativeClick(View v) {
    }
}
