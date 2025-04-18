/*
 *  Copyright (c) 2023~2025 chr_56
 */

package player.phonograph.ui.modules.setting

import lib.phonograph.localization.LanguageSettingDialog
import lib.phonograph.localization.LocalizationStore
import lib.phonograph.misc.ColorPalette
import player.phonograph.App
import player.phonograph.R
import player.phonograph.appshortcuts.DynamicShortcutManager
import player.phonograph.coil.cache.CacheStore
import player.phonograph.mechanism.StatusBarLyric
import player.phonograph.model.time.Duration
import player.phonograph.model.time.TimeIntervalCalculationMode
import player.phonograph.model.time.displayText
import player.phonograph.settings.Keys
import player.phonograph.settings.PreferenceKey
import player.phonograph.settings.Setting
import player.phonograph.settings.THEME_AUTO_LIGHTBLACK
import player.phonograph.settings.THEME_AUTO_LIGHTDARK
import player.phonograph.settings.THEME_BLACK
import player.phonograph.settings.THEME_DARK
import player.phonograph.settings.THEME_LIGHT
import player.phonograph.settings.ThemeSetting
import player.phonograph.ui.modules.setting.components.BooleanPreference
import player.phonograph.ui.modules.setting.components.ColorPreference
import player.phonograph.ui.modules.setting.components.DialogPreference
import player.phonograph.ui.modules.setting.components.ExternalPreference
import player.phonograph.ui.modules.setting.components.ListPreference
import player.phonograph.ui.modules.setting.components.SettingsGroup
import player.phonograph.ui.modules.setting.dialog.CheckUpdateIntervalDialog
import player.phonograph.ui.modules.setting.dialog.ClickModeSettingDialog
import player.phonograph.ui.modules.setting.dialog.ExternalPlayRequestSettingDialog
import player.phonograph.ui.modules.setting.dialog.HomeTabConfigDialog
import player.phonograph.ui.modules.setting.dialog.ImageSourceConfigDialog
import player.phonograph.ui.modules.setting.dialog.LastAddedPlaylistIntervalDialog
import player.phonograph.ui.modules.setting.dialog.MaterialColorPickerDialog
import player.phonograph.ui.modules.setting.dialog.MonetColorPickerDialog
import player.phonograph.ui.modules.setting.dialog.NotificationActionsConfigDialog
import player.phonograph.ui.modules.setting.dialog.NowPlayingScreenStylePreferenceDialog
import player.phonograph.ui.modules.setting.dialog.PathFilterPreferenceDialog
import player.phonograph.util.NavigationUtil
import player.phonograph.util.concurrent.lifecycleScopeOrNewOne
import player.phonograph.util.theme.tintButtons
import player.phonograph.util.warning
import androidx.activity.compose.LocalActivity
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.audiofx.AudioEffect
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.N
import android.os.Build.VERSION_CODES.N_MR1
import android.os.Build.VERSION_CODES.S
import android.os.Build.VERSION_CODES.TIRAMISU
import kotlinx.coroutines.launch

@Composable
fun PhonographPreferenceScreen() {
    Column(
        Modifier.verticalScroll(rememberScrollState())
    ) {
        SettingsGroup(titleRes = R.string.pref_header_appearance) {
            DialogPreference(
                dialog = LanguageSettingDialog::class.java,
                titleRes = R.string.app_language,
                currentValueForHint = { context ->
                    val locale = LocalizationStore.current(context)
                    locale.getDisplayName(locale)
                }
            )
            DialogPreference(
                dialog = NowPlayingScreenStylePreferenceDialog::class.java,
                titleRes = R.string.pref_title_player_style,
                summaryRes = R.string.pref_title_now_playing_screen_style,
                reset = {
                    resetPreference(it, R.string.pref_title_player_style, Keys.nowPlayingScreenStyle)
                }
            )
            DialogPreference(
                dialog = HomeTabConfigDialog::class.java,
                titleRes = R.string.library_categories,
                summaryRes = R.string.pref_summary_library_categories,
                reset = {
                    resetPreference(it, R.string.library_categories, Keys.homeTabConfig)
                }
            )
            BooleanPreference(
                key = Keys.rememberLastTab,
                titleRes = R.string.pref_title_remember_last_tab,
                summaryRes = R.string.pref_summary_remember_last_tab,
            )
            BooleanPreference(
                key = Keys.fixedTabLayout,
                titleRes = R.string.perf_title_fixed_tab_layout,
                summaryRes = R.string.pref_summary_fixed_tab_layout,
            )
        }

        SettingsGroup(titleRes = R.string.pref_header_colors) {
            if (SDK_INT >= S) MonetSetting()
            PrimaryColorPref()
            AccentColorPref()
            GeneralThemeSetting()
            if (SDK_INT >= N_MR1) ColoredAppShortcuts()
        }

        SettingsGroup(titleRes = R.string.pref_header_content) {
            DialogPreference(
                dialog = PathFilterPreferenceDialog::class.java,
                titleRes = R.string.path_filter,
                currentValueForHint = { context ->
                    with(context) {
                        val preference = Setting(context)[Keys.pathFilterExcludeMode]
                        getString(
                            if (preference.data) R.string.path_filter_excluded_mode else R.string.path_filter_included_mode
                        )
                    }
                }
            )
            DialogPreference(
                dialog = LastAddedPlaylistIntervalDialog::class.java,
                titleRes = R.string.pref_title_last_added_interval,
                currentValueForHint = { context ->
                    val resources = context.resources
                    val setting = Setting(context)
                    val calculationMode =
                        setting[Keys._lastAddedCutOffMode].data
                            .let(TimeIntervalCalculationMode.Companion::from)
                    val duration =
                        setting[Keys._lastAddedCutOffDuration].data
                            .let(Duration.Companion::from)
                    if (calculationMode != null && duration != null)
                        resources.getString(
                            R.string.time_interval_text,
                            calculationMode.displayText(resources),
                            duration.value,
                            duration.unit.displayText(resources)
                        )
                    else
                        resources.getString(R.string._default)
                },
                reset = {
                    resetPreference(
                        it,
                        R.string.pref_title_last_added_interval,
                        Keys._lastAddedCutOffMode,
                        Keys._lastAddedCutOffDuration
                    )
                }
            )
            DialogPreference(
                dialog = ClickModeSettingDialog::class.java,
                titleRes = R.string.pref_title_click_behavior,
                summaryRes = R.string.pref_summary_click_behavior,
                reset = {
                    resetPreference(
                        it,
                        R.string.pref_title_click_behavior,
                        Keys.songItemClickMode,
                        Keys.songItemClickExtraFlag,
                    )
                }
            )
            DialogPreference(
                dialog = ExternalPlayRequestSettingDialog::class.java,
                titleRes = R.string.pref_title_external_play_request,
                summaryRes = R.string.pref_summary_external_play_request,
                reset = {
                    resetPreference(
                        it,
                        R.string.pref_title_external_play_request,
                        Keys.externalPlayRequestMultipleMode,
                        Keys.externalPlayRequestSingleMode,
                        Keys.externalPlayRequestShowPrompt,
                        Keys.externalPlayRequestSilence,
                    )
                }
            )
            DialogPreference(
                dialog = ImageSourceConfigDialog::class.java,
                titleRes = R.string.image_source_config,
                reset = {
                    resetPreference(it, R.string.image_source_config, Keys.imageSourceConfig)
                }
            )
            BooleanPreference(
                key = Keys.imageCache,
                summaryRes = R.string.pref_summary_image_cache,
                titleRes = R.string.pref_title_image_cache,
            )
            ExternalPreference(titleRes = R.string.clear_image_cache) { CacheStore.clear(App.instance) }
        }


        SettingsGroup(titleRes = R.string.pref_header_player_behaviour) {
            BooleanPreference(
                key = Keys.audioDucking,
                summaryRes = R.string.pref_summary_audio_ducking,
                titleRes = R.string.pref_title_audio_ducking,
            )
            BooleanPreference(
                key = Keys.resumeAfterAudioFocusGain,
                summaryRes = R.string.pref_summary_resume_after_audio_focus_gain,
                titleRes = R.string.pref_title_resume_after_audio_focus_gain,
            )
            BooleanPreference(
                key = Keys.alwaysPlay,
                summaryRes = R.string.pref_summary_always_play,
                titleRes = R.string.pref_title_always_play,
            )
            BooleanPreference(
                key = Keys.gaplessPlayback,
                summaryRes = R.string.pref_summary_gapless_playback,
                titleRes = R.string.pref_title_gapless_playback,
            )
            BooleanPreference(
                key = Keys.broadcastCurrentPlayerState,
                summaryRes = R.string.pref_summary_broadcast_current_player_state,
                titleRes = R.string.pref_title_broadcast_current_player_state,
            )
            EqualizerSetting()
        }


        SettingsGroup(titleRes = R.string.pref_header_notification) {
            BooleanPreference(
                key = Keys.persistentPlaybackNotification,
                titleRes = R.string.pref_title_persistent_playback_notification,
                summaryRes = R.string.pref_summary_persistent_playback_notification,
            )
            // noinspection ObsoleteSdkInt
            if (SDK_INT >= N) BooleanPreference(
                key = Keys.classicNotification,
                titleRes = R.string.pref_title_classic_notification,
                summaryRes = R.string.pref_summary_classic_notification,
            )
            BooleanPreference(
                key = Keys.coloredNotification,
                titleRes = R.string.pref_title_colored_notification,
                summaryRes = R.string.pref_summary_colored_notification,
                enabled = dependOn(Keys.classicNotification) { it == true },
            )
            DialogPreference(
                dialog = NotificationActionsConfigDialog::class.java,
                titleRes = R.string.pref_title_notification_actions,
                summaryRes = R.string.pref_summary_notification_actions,
                reset = {
                    resetPreference(it, R.string.pref_title_notification_actions, Keys.notificationActions)
                }
            )
        }


        SettingsGroup(titleRes = R.string.pref_header_lyrics) {
            BooleanPreference(
                key = Keys.enableLyrics,
                titleRes = R.string.pref_title_load_lyrics,
                summaryRes = R.string.pref_summary_load_lyrics,
            )
            BooleanPreference(
                key = Keys.synchronizedLyricsShow,
                titleRes = R.string.pref_title_synchronized_lyrics_show,
                summaryRes = R.string.pref_summary_synchronized_lyrics_show,
                onCheckedChange = { newValue ->
                    if (!newValue) {
                        // clear lyrics displaying on the status bar now
                        StatusBarLyric.stopLyric()
                    }
                }
            )
            BooleanPreference(
                key = Keys.displaySynchronizedLyricsTimeAxis,
                titleRes = R.string.pref_title_display_lyrics_time_axis,
                summaryRes = R.string.pref_summary_display_lyrics_time_axis,
            )
            BooleanPreference(
                key = Keys.broadcastSynchronizedLyrics,
                titleRes = R.string.pref_title_send_lyrics,
                summaryRes = R.string.pref_summary_send_lyrics,
            )
        }

        SettingsGroup(titleRes = R.string.pref_header_compatibility) {
            BooleanPreference(
                key = Keys.alwaysUseMediaSessionToDisplayCover,
                titleRes = R.string.pref_title_always_use_media_session_to_display_cover,
                summaryRes = R.string.pref_summary_always_use_media_session_to_display_cover,
            )
            BooleanPreference(
                key = Keys.useLegacyFavoritePlaylistImpl,
                titleRes = R.string.pref_title_use_legacy_favorite_playlist_impl,
                summaryRes = R.string.pref_summary_use_legacy_favorite_playlist_impl,
            )
            BooleanPreference(
                key = Keys.useLegacyListFilesImpl,
                titleRes = R.string.use_legacy_list_Files,
            )
            BooleanPreference(
                key = Keys.useLegacyStatusBarLyricsApi,
                titleRes = R.string.pref_title_use_legacy_status_bar_lyrics_api,
                summaryRes = R.string.pref_summary_use_legacy_status_bar_lyrics_api,
            )
            BooleanPreference(
                key = Keys.disableRealTimeSearch,
                titleRes = R.string.pref_title_disable_real_time_search,
                summaryRes = R.string.pref_summary_disable_real_time_search,
            )
        }

        SettingsGroup(titleRes = R.string.check_for_updates) {
            BooleanPreference(
                key = Keys.checkUpgradeAtStartup,
                titleRes = R.string.pref_title_auto_check_for_updates,
                summaryRes = R.string.pref_summary_auto_check_for_updates,
            )
            DialogPreference(
                CheckUpdateIntervalDialog::class.java,
                titleRes = R.string.pref_title_check_for_updates_interval,
                summaryRes = R.string.pref_summary_check_for_updates_interval,
                reset = {
                    resetPreference(it, R.string.pref_title_check_for_updates_interval, Keys.checkUpdateInterval)
                }
            ) {
                val resources = it.resources
                val preference = Setting(it)[Keys.checkUpdateInterval]
                val duration = preference.data
                resources.getString(
                    R.string.time_interval_text,
                    resources.getString(R.string.interval_every),
                    duration.value,
                    duration.unit.displayText(resources)
                )
            }
        }

    }
}

//region Special Preferences


@Composable
private fun GeneralThemeSetting() {
    val themeValues: List<String> = listOf(
        THEME_AUTO_LIGHTBLACK,
        THEME_AUTO_LIGHTDARK,
        THEME_LIGHT,
        THEME_BLACK,
        THEME_DARK,
    )
    val themeNames: List<Int> = listOf(
        R.string.theme_name_auto_lightblack,
        R.string.theme_name_auto_lightdark,
        R.string.theme_name_light,
        R.string.theme_name_black,
        R.string.theme_name_dark,
    )
    val context = LocalContext.current
    ListPreference(
        key = Keys.theme,
        optionsValues = themeValues,
        optionsValuesLocalized = themeNames,
        titleRes = R.string.pref_title_general_theme,
        onChange = { _, _ ->
            ThemeSetting.updateThemeStyle(context)
        }
    )
}

@Composable
private fun PrimaryColorPref() {
    ColorPreference(
        titleRes = R.string.primary_color,
        summaryRes = R.string.primary_color_desc,
        ColorPalette.Variant.Primary
    )
}
@Composable
private fun AccentColorPref() {
    ColorPreference(
        titleRes = R.string.accent_color,
        summaryRes = R.string.accent_color_desc,
        ColorPalette.Variant.Accent
    )
}

@Composable
private fun ColorPreference(
    @StringRes titleRes: Int,
    @StringRes summaryRes: Int,
    variant: ColorPalette.Variant,
) {
    val color = when (variant) {
        ColorPalette.Variant.Primary -> MaterialTheme.colors.primary
        ColorPalette.Variant.Accent  -> MaterialTheme.colors.secondary
    }
    val context = LocalContext.current
    ColorPreference(titleRes, summaryRes, color) {
        if (SDK_INT >= S && Setting(context)[Keys.enableMonet].data) {
            MonetColorPickerDialog.showColorChooserDialog(context, variant)
        } else {
            MaterialColorPickerDialog.showColorChooserDialog(context, color.toArgb(), variant)
        }
    }
}

@Composable
private fun MonetSetting() {
    BooleanPreference(
        key = Keys.enableMonet,
        titleRes = R.string.pref_title_enable_monet,
        summaryRes = R.string.pref_summary_enable_monet,
        onCheckedChange = {
            if (SDK_INT >= N_MR1) DynamicShortcutManager(App.instance).updateDynamicShortcuts()
        }
    )
}

@Composable
private fun ColoredAppShortcuts() {
    BooleanPreference(
        key = Keys.coloredAppShortcuts,
        titleRes = R.string.pref_title_app_shortcuts,
        summaryRes = R.string.pref_summary_colored_app_shortcuts,
        onCheckedChange = {
            if (SDK_INT >= N_MR1) DynamicShortcutManager(App.instance).updateDynamicShortcuts()
        }
    )
}

@Composable
private fun EqualizerSetting() {
    val activity = if (!LocalInspectionMode.current) LocalActivity.current else null
    var hasEqualizer by remember { mutableStateOf(false) }
    if (!LocalInspectionMode.current) {
        LaunchedEffect(activity) {
            val packageManager = activity?.packageManager
            val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
            }
            val resolveInfo = if (packageManager != null) {
                if (SDK_INT > TIRAMISU) {
                    packageManager.resolveActivity(intent, PackageManager.ResolveInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    packageManager.resolveActivity(intent, 0)
                }
            } else null

            hasEqualizer = resolveInfo != null
        }
    }

    ExternalPreference(
        titleRes = R.string.equalizer,
        summaryRes = if (hasEqualizer) R.string.no_equalizer else 0
    ) {
        if (activity != null) {
            NavigationUtil.openEqualizer(activity)
        } else {
            warning("Equalizer", "can not open Equalizer Setting")
        }
    }
}

//endregion

@Composable
private fun <T> dependOn(key: PreferenceKey<T>, predicate: (T) -> Boolean): Boolean {
    return if (LocalInspectionMode.current) {
        false
    } else {
        val context = LocalContext.current
        val preference = remember { Setting(context)[key] }
        val state by preference.flow.collectAsState(preference.default)
        predicate(state)
    }
}


private fun resetPreference(context: Context, @StringRes what: Int, vararg keys: PreferenceKey<*>) {
    AlertDialog.Builder(context)
        .setTitle(context.getString(R.string.reset_action))
        .setMessage(context.getString(what))
        .setPositiveButton(android.R.string.ok) { _, _ ->
            val targets = keys.map { Setting(context)[it] }
            context.lifecycleScopeOrNewOne().launch {
                for (preference in targets) {
                    preference.reset()
                }
            }
        }
        .setNegativeButton(android.R.string.cancel) { _, _ -> }
        .show().tintButtons()
}
