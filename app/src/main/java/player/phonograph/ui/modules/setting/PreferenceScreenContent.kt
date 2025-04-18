/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.setting

import player.phonograph.App
import player.phonograph.R
import player.phonograph.coil.cache.CacheStore
import player.phonograph.mechanism.StatusBarLyric
import player.phonograph.model.time.Duration
import player.phonograph.model.time.TimeIntervalCalculationMode
import player.phonograph.model.time.displayText
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.ui.modules.setting.components.BooleanPreference
import player.phonograph.ui.modules.setting.components.DialogPreference
import player.phonograph.ui.modules.setting.components.ExternalPreference
import player.phonograph.ui.modules.setting.components.SettingsGroup
import player.phonograph.ui.modules.setting.dialog.ClickModeSettingDialog
import player.phonograph.ui.modules.setting.dialog.ExternalPlayRequestSettingDialog
import player.phonograph.ui.modules.setting.dialog.ImageSourceConfigDialog
import player.phonograph.ui.modules.setting.dialog.LastAddedPlaylistIntervalDialog
import player.phonograph.ui.modules.setting.dialog.PathFilterPreferenceDialog
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun PreferenceScreenContent() {
    Column(
        Modifier.verticalScroll(rememberScrollState())
    ) {
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
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.systemBars))
    }
}