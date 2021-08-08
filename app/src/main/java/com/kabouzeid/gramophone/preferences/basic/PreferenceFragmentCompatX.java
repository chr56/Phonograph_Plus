package com.kabouzeid.gramophone.preferences.basic;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.kabouzeid.gramophone.preferences.basic.dialog.EditTextPreferenceDialogFragmentCompatX;
import com.kabouzeid.gramophone.preferences.basic.dialog.ListPreferenceDialogFragmentCompatX;
import com.kabouzeid.gramophone.preferences.basic.dialog.PreferenceDialogFragmentX;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public abstract class PreferenceFragmentCompatX extends PreferenceFragmentCompat {
    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        if (this.getCallbackFragment() instanceof OnPreferenceDisplayDialogCallback) {
            ((OnPreferenceDisplayDialogCallback) this.getCallbackFragment()).onPreferenceDisplayDialog(this, preference);
            return;
        }

        if (this.getActivity() instanceof OnPreferenceDisplayDialogCallback) {
            ((OnPreferenceDisplayDialogCallback) this.getActivity()).onPreferenceDisplayDialog(this, preference);
            return;
        }

        if (this.getFragmentManager().findFragmentByTag("android.support.v7.preference.PreferenceFragment.DIALOG") == null) {
            DialogFragment dialogFragment = onCreatePreferenceDialog(preference);

            if (dialogFragment != null) {
                dialogFragment.setTargetFragment(this, 0);
                dialogFragment.show(this.getFragmentManager(), "android.support.v7.preference.PreferenceFragment.DIALOG");
                return;
            }
        }

        super.onDisplayPreferenceDialog(preference);
    }

    @Nullable
    public DialogFragment onCreatePreferenceDialog(Preference preference) {
        if (preference instanceof EditTextPreferenceX) {
            return EditTextPreferenceDialogFragmentCompatX.newInstance(preference.getKey());
        } else if (preference instanceof ListPreferenceX) {
            return ListPreferenceDialogFragmentCompatX.newInstance(preference.getKey());
        } else if (preference instanceof DialogPreferenceX) {
            return PreferenceDialogFragmentX.newInstance(preference.getKey());
        }
        return null;
    }
}
