/*
 * Copyright (c) 2021 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.ui.fragments

import com.afollestad.materialdialogs.MaterialDialog
import lib.phonograph.localization.LanguageSettingDialog
import lib.phonograph.localization.Localization
import lib.phonograph.preference.ColorPreferenceX
import lib.phonograph.preference.DialogPreferenceX
import lib.phonograph.preference.EditTextPreferenceX
import lib.phonograph.preference.ListPreferenceX
import lib.phonograph.preference.SwitchPreferenceX
import lib.phonograph.preference.dialog.EditTextPreferenceDialogFragmentCompatX
import lib.phonograph.preference.dialog.ListPreferenceDialogFragmentCompatX
import lib.phonograph.preference.dialog.PreferenceDialogFragmentX
import mt.pref.ThemeColor
import mt.util.color.darkenColor
import player.phonograph.R
import player.phonograph.appshortcuts.DynamicShortcutManager
import player.phonograph.mechanism.StatusBarLyric
import player.phonograph.mechanism.setting.HomeTabConfig
import player.phonograph.mechanism.setting.NowPlayingScreenConfig
import player.phonograph.mechanism.setting.StyleConfig
import player.phonograph.preferences.HomeTabConfigDialog
import player.phonograph.preferences.NowPlayingScreenPreferenceDialog
import player.phonograph.settings.Setting
import player.phonograph.ui.dialogs.ClickModeSettingDialog
import player.phonograph.ui.dialogs.ImageSourceConfigDialog
import player.phonograph.ui.dialogs.PathFilterDialog
import player.phonograph.util.NavigationUtil
import player.phonograph.util.theme.applyMonet
import util.phonograph.misc.ColorChooserListener
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenStarted
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.TwoStatePreference
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.media.audiofx.AudioEffect
import android.os.Build
import android.os.Bundle
import android.view.View
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences)
    }

    private fun onCreatePreferenceDialog(preference: Preference): DialogFragment? =
        when (val key = preference.key) {
            getString(R.string.preference_key_app_language)        -> LanguageSettingDialog()
            getString(R.string.preference_key_blacklist)           -> PathFilterDialog()
            getString(R.string.preference_key_home_tab_config)     -> HomeTabConfigDialog()
            getString(R.string.preference_key_now_playing_screen)  -> NowPlayingScreenPreferenceDialog()
            getString(R.string.preference_key_click_behavior)      -> ClickModeSettingDialog()
            getString(R.string.preference_key_image_source_config) -> ImageSourceConfigDialog()
            else                                                   -> {
                when (preference) {
                    is EditTextPreferenceX ->
                        EditTextPreferenceDialogFragmentCompatX.newInstance(key)
                    is ListPreferenceX     ->
                        ListPreferenceDialogFragmentCompatX.newInstance(key)
                    is DialogPreferenceX   -> {
                        PreferenceDialogFragmentX.newInstance(key)
                    }
                    else                   -> null
                }
            }
        }

    //    @SuppressLint("RestrictedApi")
    override fun onDisplayPreferenceDialog(preference: Preference) {
//        if (this.callbackFragment is OnPreferenceDisplayDialogCallback) {
//            (this.callbackFragment as OnPreferenceDisplayDialogCallback)
//                .onPreferenceDisplayDialog(this, preference)
//            return
//        }
//        if (this.activity is OnPreferenceDisplayDialogCallback) {
//            (this.activity as OnPreferenceDisplayDialogCallback?)!!
//                .onPreferenceDisplayDialog(this, preference)
//            return
//        }
        val tag = "androidx.preference.PreferenceFragmentCompat.DIALOG"

//        if (this.parentFragmentManager.findFragmentByTag(tag) == null) {
        val dialogFragment = onCreatePreferenceDialog(preference)
        if (dialogFragment != null) {
            dialogFragment.setTargetFragment(this, 0)
            dialogFragment.show(this.parentFragmentManager, tag)
            return
        }
//        }
        super.onDisplayPreferenceDialog(preference)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Setting.instance.observe(
            this, arrayOf(
                Setting.NOW_PLAYING_SCREEN_ID,
                Setting.PATH_FILTER_EXCLUDE_MODE,
                Setting.CLASSIC_NOTIFICATION,
                Setting.BROADCAST_SYNCHRONIZED_LYRICS,
            )
        ) { sharedPreferences, key ->
            lifecycleScope.launch {
                lifecycle.whenStarted {
                    when (key) {
                        Setting.NOW_PLAYING_SCREEN_ID         -> updateNowPlayingScreenSummary()
                        Setting.PATH_FILTER_EXCLUDE_MODE      -> updatePathFilterExcludeMode()
                        Setting.CLASSIC_NOTIFICATION          ->
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                findPreference<Preference>("colored_notification")!!.isEnabled =
                                    sharedPreferences.getBoolean(key, false)
                            }
                        Setting.BROADCAST_SYNCHRONIZED_LYRICS -> {
                            delay(200)
                            // clear lyrics displaying on the statusbar now
                            StatusBarLyric.stopLyric()
                        }
                    }
                }

            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(requireView(), savedInstanceState)
        listView.setPadding(0, 0, 0, 0)
        invalidateSettings()
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        if (preference.key == "reset_home_pages_tab_config") {
            MaterialDialog(requireContext())
                .title(R.string.pref_title_reset_home_pages_tab_config)
                .message(
                    text = "${getString(R.string.pref_summary_reset_home_pages_tab_config)}\n${
                        getString(
                            R.string.are_you_sure
                        )
                    }"
                )
                .positiveButton {
                    HomeTabConfig.resetHomeTabConfig()
                }
                .negativeButton {
                    it.dismiss()
                }
                .show()
        }
        return super.onPreferenceTreeClick(preference)
    }

    private fun updateNowPlayingScreenSummary() {
        findPreference<Preference>(getString(R.string.preference_key_now_playing_screen))?.setSummary(
            NowPlayingScreenConfig.nowPlayingScreen.titleRes
        )
    }

    private fun updatePathFilterExcludeMode() {
        findPreference<Preference>(getString(R.string.preference_key_blacklist))?.summary =
            if (Setting.instance.pathFilterExcludeMode) {
                "${getString(R.string.path_filter_excluded_mode)} - \n${getString(R.string.pref_summary_path_filter_excluded_mode)}"
            } else {
                "${getString(R.string.path_filter_included_mode)} - \n${getString(R.string.pref_summary_path_filter_included_mode)}"
            }
    }

    fun invalidateSettings() {
        //
        val generalTheme = findPreference<Preference>("general_theme")!!
        setSummary(generalTheme)
        generalTheme.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _: Preference?, newValue: Any? ->
                setSummary(generalTheme, newValue!!)
                ThemeColor.editTheme(requireContext()).markChanged()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                    // Set the new theme so that updateAppShortcuts can pull it
                    requireActivity().setTheme(StyleConfig.getThemeResFromPrefValue(newValue as String?))
                    DynamicShortcutManager(requireContext()).updateDynamicShortcuts()
                }
                requireActivity().recreate()
                true
            }

        val appLanguage =
            findPreference<DialogPreferenceX>(getString(R.string.preference_key_app_language))
        setSummary(appLanguage as Preference, Localization.currentLocale(requireContext()).displayLanguage)

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

        val enableMonetSetting = findPreference<Preference>("enable_monet") as SwitchPreferenceX
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            enableMonetSetting.isVisible = false // hide below Android S
        } else {
            enableMonetSetting.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { preference: Preference, newValue: Any? ->
                    if (newValue as Boolean) {
                        applyMonet(preference.context, true)
                        DynamicShortcutManager(preference.context).updateDynamicShortcuts()
                        requireActivity().recreate()
                    }
                    true
                }
        }

        val primaryColorPref = findPreference<Preference>("primary_color") as ColorPreferenceX
        primaryColorPref.setColor(primaryColor, darkenColor(primaryColor))
        primaryColorPref.onPreferenceClickListener =
            ColorChooserListener(
                requireActivity(),
                primaryColor,
                ColorChooserListener.MODE_PRIMARY_COLOR
            )

        val accentColorPref = findPreference<Preference>("accent_color") as ColorPreferenceX
        accentColorPref.setColor(accentColor, darkenColor(accentColor))
        accentColorPref.onPreferenceClickListener =
            ColorChooserListener(
                requireActivity(),
                accentColor,
                ColorChooserListener.MODE_ACCENT_COLOR
            )

        //
        val colorNavBar = findPreference<Preference>("should_color_navigation_bar") as TwoStatePreference
        colorNavBar.isChecked = ThemeColor.coloredNavigationBar(requireActivity())
        colorNavBar.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _: Preference?, newValue: Any? ->
                ThemeColor.editTheme(requireActivity())
                    .coloredNavigationBar(newValue as Boolean)
                    .commit()
                requireActivity().recreate()
                true
            }

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
                    DynamicShortcutManager(requireContext()).updateDynamicShortcuts()
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
        updatePathFilterExcludeMode()
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

}
