package com.kabouzeid.phonograph.ui.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.audiofx.AudioEffect;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.TwoStatePreference;

import com.kabouzeid.phonograph.R;
import com.kabouzeid.phonograph.appshortcuts.DynamicShortcutManager;
import com.kabouzeid.phonograph.preferences.BlacklistPreferenceDialog;
import com.kabouzeid.phonograph.preferences.BlacklistPreferenceX;
import com.kabouzeid.phonograph.preferences.LibraryPreferenceDialog;
import com.kabouzeid.phonograph.preferences.LibraryPreferenceX;
import com.kabouzeid.phonograph.preferences.NowPlayingScreenPreferenceDialog;
import com.kabouzeid.phonograph.preferences.NowPlayingScreenPreferenceX;
import com.kabouzeid.phonograph.preferences.basic.ColorPreferenceX;
import com.kabouzeid.phonograph.preferences.basic.DialogPreferenceX;
import com.kabouzeid.phonograph.preferences.basic.EditTextPreferenceX;
import com.kabouzeid.phonograph.preferences.basic.ListPreferenceX;
import com.kabouzeid.phonograph.preferences.basic.dialog.EditTextPreferenceDialogFragmentCompatX;
import com.kabouzeid.phonograph.preferences.basic.dialog.ListPreferenceDialogFragmentCompatX;
import com.kabouzeid.phonograph.preferences.basic.dialog.PreferenceDialogFragmentX;
import com.kabouzeid.phonograph.util.ColorChooserListener;
import com.kabouzeid.phonograph.util.NavigationUtil;
import com.kabouzeid.phonograph.util.PreferenceUtil;

import chr_56.MDthemer.core.ThemeColor;
import chr_56.MDthemer.util.ColorUtil;

public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static void setSummary(@NonNull Preference preference) {
        setSummary(preference, PreferenceManager
                .getDefaultSharedPreferences(preference.getContext())
                .getString(preference.getKey(), ""));
    }

    private static void setSummary(Preference preference, @NonNull Object value) {
        String stringValue = value.toString();

        if (preference instanceof ListPreference) {
            ListPreference listPreference = (ListPreference) preference;
            int index = listPreference.findIndexOfValue(stringValue);
            preference.setSummary(
                    index >= 0
                            ? listPreference.getEntries()[index]
                            : null);
        } else {
            preference.setSummary(stringValue);
        }
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        addPreferencesFromResource(R.xml.pref_library);
        addPreferencesFromResource(R.xml.pref_colors);
        addPreferencesFromResource(R.xml.pref_notification);
        addPreferencesFromResource(R.xml.pref_now_playing_screen);
        addPreferencesFromResource(R.xml.pref_images);
        addPreferencesFromResource(R.xml.pref_lockscreen);
        addPreferencesFromResource(R.xml.pref_audio);
        addPreferencesFromResource(R.xml.pref_playlists);
        addPreferencesFromResource(R.xml.pref_blacklist);
    }

    @Nullable
    public DialogFragment onCreatePreferenceDialog(Preference preference) {
               if (preference instanceof NowPlayingScreenPreferenceX) {
            return NowPlayingScreenPreferenceDialog.newInstance();
        } else if (preference instanceof BlacklistPreferenceX) {
            return BlacklistPreferenceDialog.newInstance();
        } else if (preference instanceof LibraryPreferenceX) {
            return LibraryPreferenceDialog.newInstance();
        } else if (preference instanceof EditTextPreferenceX) {
            return EditTextPreferenceDialogFragmentCompatX.newInstance(preference.getKey());
        } else if (preference instanceof ListPreferenceX) {
            return ListPreferenceDialogFragmentCompatX.newInstance(preference.getKey());
        } else if (preference instanceof DialogPreferenceX) {
            return PreferenceDialogFragmentX.newInstance(preference.getKey());
        }
        return null;
    }
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

        if (this.getFragmentManager().findFragmentByTag("androidx.preference.PreferenceFragmentCompat.DIALOG") == null) {
            DialogFragment dialogFragment = onCreatePreferenceDialog(preference);

            if (dialogFragment != null) {
                dialogFragment.setTargetFragment(this, 0);
                dialogFragment.show(this.getFragmentManager(), "androidx.preference.PreferenceFragmentCompat.DIALOG");
                return;
            }
        }

        super.onDisplayPreferenceDialog(preference);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getListView().setPadding(0, 0, 0, 0);
        invalidateSettings();
        PreferenceUtil.getInstance(getActivity()).registerOnSharedPreferenceChangedListener(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        PreferenceUtil.getInstance(getActivity()).unregisterOnSharedPreferenceChangedListener(this);
    }

    public void invalidateSettings() {
        final Preference generalTheme = findPreference("general_theme");
        setSummary(generalTheme);
        generalTheme.setOnPreferenceChangeListener((preference, o) -> {
            String themeName = (String) o;

            setSummary(generalTheme, o);

            ThemeColor.markChanged(getActivity());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                // Set the new theme so that updateAppShortcuts can pull it
                getActivity().setTheme(PreferenceUtil.getThemeResFromPrefValue(themeName));
                new DynamicShortcutManager(getActivity()).updateDynamicShortcuts();
            }

            getActivity().recreate();
            return true;
        });

        final Preference autoDownloadImagesPolicy = findPreference("auto_download_images_policy");
        setSummary(autoDownloadImagesPolicy);
        autoDownloadImagesPolicy.setOnPreferenceChangeListener((preference, o) -> {
            setSummary(autoDownloadImagesPolicy, o);
            return true;
        });

        final ColorPreferenceX primaryColorPref = (ColorPreferenceX) findPreference("primary_color");
        final int primaryColor = ThemeColor.primaryColor(getActivity());
        primaryColorPref.setColor(primaryColor, ColorUtil.darkenColor(primaryColor));
        primaryColorPref.setOnPreferenceClickListener(
                new ColorChooserListener(getActivity(),primaryColor, ColorChooserListener.MODE_PRIMARY_COLOR)
        );


        final ColorPreferenceX accentColorPref = (ColorPreferenceX) findPreference("accent_color");
        final int accentColor = ThemeColor.accentColor(getActivity());
        accentColorPref.setColor(accentColor, ColorUtil.darkenColor(accentColor));
        accentColorPref.setOnPreferenceClickListener(
                new ColorChooserListener(getActivity(),accentColor, ColorChooserListener.MODE_ACCENT_COLOR)
        );

        TwoStatePreference colorNavBar = (TwoStatePreference) findPreference("should_color_navigation_bar");
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            colorNavBar.setVisible(false);
        } else {
            colorNavBar.setChecked(ThemeColor.coloredNavigationBar(getActivity()));
            colorNavBar.setOnPreferenceChangeListener((preference, newValue) -> {
                ThemeColor.editTheme(getActivity())
                        .coloredNavigationBar((Boolean) newValue)
                        .commit();
                getActivity().recreate();
                return true;
            });
        }

        final TwoStatePreference classicNotification = (TwoStatePreference) findPreference("classic_notification");
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            classicNotification.setVisible(false);
        } else {
            classicNotification.setChecked(PreferenceUtil.getInstance(getActivity()).classicNotification());
            classicNotification.setOnPreferenceChangeListener((preference, newValue) -> {
                // Save preference
                PreferenceUtil.getInstance(getActivity()).setClassicNotification((Boolean) newValue);
                return true;
            });
        }

        final TwoStatePreference coloredNotification = (TwoStatePreference) findPreference("colored_notification");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            coloredNotification.setEnabled(PreferenceUtil.getInstance(getActivity()).classicNotification());
        } else {
            coloredNotification.setChecked(PreferenceUtil.getInstance(getActivity()).coloredNotification());
            coloredNotification.setOnPreferenceChangeListener((preference, newValue) -> {
                // Save preference
                PreferenceUtil.getInstance(getActivity()).setColoredNotification((Boolean) newValue);
                return true;
            });
        }

        final TwoStatePreference colorAppShortcuts = (TwoStatePreference) findPreference("should_color_app_shortcuts");
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
            colorAppShortcuts.setVisible(false);
        } else {
            colorAppShortcuts.setChecked(PreferenceUtil.getInstance(getActivity()).coloredAppShortcuts());
            colorAppShortcuts.setOnPreferenceChangeListener((preference, newValue) -> {
                // Save preference
                PreferenceUtil.getInstance(getActivity()).setColoredAppShortcuts((Boolean) newValue);

                // Update app shortcuts
                new DynamicShortcutManager(getActivity()).updateDynamicShortcuts();

                return true;
            });
        }

        final Preference equalizer = findPreference("equalizer");
        if (!hasEqualizer()) {
            equalizer.setEnabled(false);
            equalizer.setSummary(getResources().getString(R.string.no_equalizer));
        }
        equalizer.setOnPreferenceClickListener(preference -> {
            NavigationUtil.openEqualizer(getActivity());
            return true;
        });

        updateNowPlayingScreenSummary();
    }

    private boolean hasEqualizer() {
        final Intent effects = new Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
        PackageManager pm = getActivity().getPackageManager();
        ResolveInfo ri = pm.resolveActivity(effects, 0);
        return ri != null;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case PreferenceUtil.NOW_PLAYING_SCREEN_ID:
                updateNowPlayingScreenSummary();
                break;
            case PreferenceUtil.CLASSIC_NOTIFICATION:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    findPreference("colored_notification").setEnabled(sharedPreferences.getBoolean(key, false));
                }
                break;
        }
    }

    private void updateNowPlayingScreenSummary() {
        findPreference("now_playing_screen_id").setSummary(PreferenceUtil.getInstance(getActivity()).getNowPlayingScreen().titleRes);
    }
}

