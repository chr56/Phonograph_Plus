package com.kabouzeid.appthemehelper.common.prefs.supportv7;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.kabouzeid.appthemehelper.common.prefs.supportv7.dialogs.ATEEditTextPreferenceDialogFragmentCompat;
import com.kabouzeid.appthemehelper.common.prefs.supportv7.dialogs.ATEListPreferenceDialogFragmentCompat;
import com.kabouzeid.appthemehelper.common.prefs.supportv7.dialogs.ATEPreferenceDialogFragment;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public abstract class ATEPreferenceFragmentCompat extends PreferenceFragmentCompat {
    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        if (this.getCallbackFragment() instanceof PreferenceFragmentCompat.OnPreferenceDisplayDialogCallback) {
            ((PreferenceFragmentCompat.OnPreferenceDisplayDialogCallback) this.getCallbackFragment()).onPreferenceDisplayDialog(this, preference);
            return;
        }

        if (this.getActivity() instanceof PreferenceFragmentCompat.OnPreferenceDisplayDialogCallback) {
            ((PreferenceFragmentCompat.OnPreferenceDisplayDialogCallback) this.getActivity()).onPreferenceDisplayDialog(this, preference);
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
        if (preference instanceof ATEEditTextPreference) {
            return ATEEditTextPreferenceDialogFragmentCompat.newInstance(preference.getKey());
        } else if (preference instanceof ATEListPreference) {
            return ATEListPreferenceDialogFragmentCompat.newInstance(preference.getKey());
        } else if (preference instanceof ATEDialogPreference) {
            return ATEPreferenceDialogFragment.newInstance(preference.getKey());
        }
        return null;
    }
}
