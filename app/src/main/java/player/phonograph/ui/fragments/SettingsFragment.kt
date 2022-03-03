/*
 * Copyright (c) 2021 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.ui.fragments

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.audiofx.AudioEffect
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.preference.*
import lib.phonograph.preference.ColorPreferenceX
import lib.phonograph.preference.DialogPreferenceX
import lib.phonograph.preference.EditTextPreferenceX
import lib.phonograph.preference.ListPreferenceX
import lib.phonograph.preference.dialog.EditTextPreferenceDialogFragmentCompatX
import lib.phonograph.preference.dialog.ListPreferenceDialogFragmentCompatX
import lib.phonograph.preference.dialog.PreferenceDialogFragmentX
import player.phonograph.R
import player.phonograph.appshortcuts.DynamicShortcutManager
import player.phonograph.preferences.*
import player.phonograph.settings.Setting
import player.phonograph.util.ColorChooserListener
import player.phonograph.util.NavigationUtil
import util.mdcolor.pref.ThemeColor
import util.mddesign.util.ColorUtil

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences)
    }

    private fun onCreatePreferenceDialog(preference: Preference): DialogFragment? {
        return when (preference) {
            is NowPlayingScreenPreferenceX ->
                NowPlayingScreenPreferenceDialog.newInstance()
            is BlacklistPreferenceX ->
                BlacklistPreferenceDialog.newInstance()
            is LibraryPreferenceX ->
                LibraryPreferenceDialog.newInstance()
            is HomeTabConfigPreferenceX ->
                HomeTabConfigDialog.newInstance()
            is EditTextPreferenceX ->
                EditTextPreferenceDialogFragmentCompatX.newInstance(preference.getKey())
            is ListPreferenceX ->
                ListPreferenceDialogFragmentCompatX.newInstance(preference.getKey())
            is DialogPreferenceX ->
                PreferenceDialogFragmentX.newInstance(preference.getKey())
            else -> null
        }
    }
    // todo
    @SuppressLint("RestrictedApi")
    override fun onDisplayPreferenceDialog(preference: Preference?) {
        if (this.callbackFragment is OnPreferenceDisplayDialogCallback) {
            (this.callbackFragment as OnPreferenceDisplayDialogCallback)
                .onPreferenceDisplayDialog(this, preference)
            return
        }
        if (this.activity is OnPreferenceDisplayDialogCallback) {
            (this.activity as OnPreferenceDisplayDialogCallback?)!!
                .onPreferenceDisplayDialog(this, preference)
            return
        }
        if (this.parentFragmentManager.findFragmentByTag("androidx.preference.PreferenceFragmentCompat.DIALOG") == null) {
            val dialogFragment = onCreatePreferenceDialog(preference!!)
            if (dialogFragment != null) {
                dialogFragment.setTargetFragment(this, 0)
                dialogFragment.show(
                    this.parentFragmentManager, "androidx.preference.PreferenceFragmentCompat.DIALOG"
                )
                return
            }
        }
        super.onDisplayPreferenceDialog(preference)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(requireView(), savedInstanceState)
        listView.setPadding(0, 0, 0, 0)
        invalidateSettings()
        Setting.instance.registerOnSharedPreferenceChangedListener(SharedPreferenceChangeListener())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Setting.instance.unregisterOnSharedPreferenceChangedListener(SharedPreferenceChangeListener())
    }

    private fun updateNowPlayingScreenSummary() {
        findPreference<Preference>("now_playing_screen_id")!!
            .setSummary(Setting.instance.nowPlayingScreen.titleRes)
    }

    fun invalidateSettings() {

        //
        val generalTheme = findPreference<Preference>("general_theme")!!
        setSummary(generalTheme)
        generalTheme.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _: Preference?, o: Any? ->
                val themeName = o as String?

                setSummary(generalTheme, o!!)

                ThemeColor.markChanged(requireActivity())

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                    // Set the new theme so that updateAppShortcuts can pull it
                    requireActivity().setTheme(Setting.getThemeResFromPrefValue(themeName))
                    DynamicShortcutManager(activity).updateDynamicShortcuts()
                }
                requireActivity().recreate()
                true
            }

        //
        val autoDownloadImagesPolicy = findPreference<Preference>("auto_download_images_policy")
        setSummary(autoDownloadImagesPolicy!!)
        autoDownloadImagesPolicy.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _: Preference?, o: Any? ->
                setSummary(autoDownloadImagesPolicy, o!!)
                true
            }

        // Theme
        val primaryColor = ThemeColor.primaryColor(requireActivity())
        val accentColor = ThemeColor.accentColor(requireActivity())

        val primaryColorPref = findPreference<Preference>("primary_color") as ColorPreferenceX?
        primaryColorPref!!.setColor(primaryColor, ColorUtil.darkenColor(primaryColor))
        primaryColorPref.onPreferenceClickListener =
            ColorChooserListener(requireActivity(), primaryColor, ColorChooserListener.MODE_PRIMARY_COLOR)

        val accentColorPref = findPreference<Preference>("accent_color") as ColorPreferenceX?
        accentColorPref!!.setColor(accentColor, ColorUtil.darkenColor(accentColor))
        accentColorPref.onPreferenceClickListener =
            ColorChooserListener(requireActivity(), accentColor, ColorChooserListener.MODE_ACCENT_COLOR)

        //
        val colorNavBar = findPreference<Preference>("should_color_navigation_bar") as TwoStatePreference?
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
//            colorNavBar!!.isVisible = false
//        } else {
        colorNavBar!!.isChecked = ThemeColor.coloredNavigationBar(requireActivity())
        colorNavBar.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _: Preference?, newValue: Any? ->
                ThemeColor.editTheme(requireActivity())
                    .coloredNavigationBar((newValue as Boolean?)!!)
                    .commit()
                requireActivity().recreate()
                true
            }
//        }

        //
        val classicNotification = findPreference<Preference>("classic_notification") as TwoStatePreference?
        @SuppressLint("ObsoleteSdkInt")
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            classicNotification!!.isVisible = false
        } else {
            classicNotification!!.isChecked = Setting.instance.classicNotification
            classicNotification.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { _: Preference?, newValue: Any? ->
                    // Save preference
                    Setting.instance.classicNotification = newValue as Boolean
                    true
                }
        }

        //
        val coloredNotification = findPreference<Preference>("colored_notification") as TwoStatePreference?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            coloredNotification!!.isEnabled = Setting.instance.classicNotification
        } else {
            coloredNotification!!.isChecked = Setting.instance.coloredNotification
            coloredNotification.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { _: Preference?, newValue: Any? ->
                    // Save preference
                    Setting.instance.coloredNotification = newValue as Boolean
                    true
                }
        }
        val colorAppShortcuts = findPreference<Preference>("should_color_app_shortcuts") as TwoStatePreference?
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
            colorAppShortcuts!!.isVisible = false
        } else {
            colorAppShortcuts!!.isChecked = Setting.instance.coloredAppShortcuts
            colorAppShortcuts.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { _: Preference?, newValue: Any? ->
                    // Save preference
                    Setting.instance.coloredAppShortcuts = newValue as Boolean
                    // Update app shortcuts
                    DynamicShortcutManager(activity).updateDynamicShortcuts()
                    true
                }
        }

        //
        val equalizer = findPreference<Preference>("equalizer")
        if (!hasEqualizer(this)) {
            equalizer!!.isEnabled = false
            equalizer.summary = resources.getString(R.string.no_equalizer)
        }
        equalizer!!.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                NavigationUtil.openEqualizer(requireActivity())
                true
            }

        //
        updateNowPlayingScreenSummary()
    }

    companion object {

        private fun setSummary(preference: Preference) {
            setSummary(
                preference,
                PreferenceManager.getDefaultSharedPreferences(preference.context)
                    .getString(preference.key, "")!!
            )
        }
        private fun setSummary(preference: Preference, value: Any) {
            val stringValue = value.toString()
            if (preference is ListPreference) {
                preference.findIndexOfValue(stringValue).also { index ->
                    preference.summary =
                        if (index >= 0) preference.entries[index] else ""
                }
            } else {
                preference.summary = stringValue
            }
        }

        private fun hasEqualizer(fragmentRequired: SettingsFragment): Boolean {
            val effects = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
            val pm: PackageManager = fragmentRequired.requireActivity().packageManager
            val ri = pm.resolveActivity(effects, 0)
            return ri != null
        }
    }

    private inner class SharedPreferenceChangeListener :
        SharedPreferences.OnSharedPreferenceChangeListener {

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
            when (key) {
                Setting.NOW_PLAYING_SCREEN_ID -> updateNowPlayingScreenSummary()
                Setting.CLASSIC_NOTIFICATION ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        findPreference<Preference>("colored_notification")!!.isEnabled =
                            sharedPreferences.getBoolean(key, false)
                    }
                Setting.BROADCAST_SYNCHRONIZED_LYRICS ->
                    // clear lyrics displaying on the statusbar now
                    player.phonograph.App.instance.lyricsService.stopLyric()
            }
        }
    }
}
