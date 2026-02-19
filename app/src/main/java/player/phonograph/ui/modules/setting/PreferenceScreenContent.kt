/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.setting

import player.phonograph.App
import player.phonograph.R
import player.phonograph.coil.cache.CacheStore
import player.phonograph.mechanism.StatusBarLyric
import player.phonograph.model.lyrics.LYRICS_ALIGN_CENTER
import player.phonograph.model.lyrics.LYRICS_ALIGN_LEFT
import player.phonograph.model.lyrics.LYRICS_ALIGN_RIGHT
import player.phonograph.model.time.Duration
import player.phonograph.model.time.TimeIntervalCalculationMode
import player.phonograph.model.time.displayText
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.ui.modules.explorer.PathSelectorRequester
import player.phonograph.ui.modules.setting.components.BooleanPreference
import player.phonograph.ui.modules.setting.components.DialogPreference
import player.phonograph.ui.modules.setting.components.ExternalPreference
import player.phonograph.ui.modules.setting.components.FloatPreference
import player.phonograph.ui.modules.setting.components.ListPreference
import player.phonograph.ui.modules.setting.components.SettingsGroup
import player.phonograph.ui.modules.setting.dialog.ClickModeSettingDialog
import player.phonograph.ui.modules.setting.dialog.ExternalPlayRequestSettingDialog
import player.phonograph.ui.modules.setting.dialog.ImageSourceConfigDialog
import player.phonograph.ui.modules.setting.dialog.LastAddedPlaylistIntervalDialog
import player.phonograph.ui.modules.setting.dialog.PathFilterEditorDialog
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import java.io.File

@Composable
fun PreferenceScreenContent() {
    val context = LocalContext.current
    Column(
        Modifier.verticalScroll(rememberScrollState())
    ) {
        SettingsGroup(titleRes = R.string.pref_header_path_filter) {
            BooleanPreference(
                key = Keys.pathFilterExcludeMode,
                titleRes = R.string.path_filter,
                currentValueForHint = { context, mode ->
                    context.getString(if (mode) R.string.path_filter_excluded_mode else R.string.path_filter_included_mode)
                }
            )
            DialogPreference(
                dialog = PathFilterEditorDialog.ExcludedMode::class.java,
                titleRes = R.string.label_excluded_paths,
                summaryRes = R.string.pref_summary_path_filter_excluded_mode,
                enabled = dependOn(Keys.pathFilterExcludeMode) { it == true }
            )
            DialogPreference(
                dialog = PathFilterEditorDialog.IncludedMode::class.java,
                titleRes = R.string.label_included_paths,
                summaryRes = R.string.pref_summary_path_filter_included_mode,
                enabled = dependOn(Keys.pathFilterExcludeMode) { it == false }
            )
        }
        SettingsGroup(titleRes = R.string.pref_header_images) {
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
            ExternalPreference(titleRes = R.string.action_clear_image_cache) { CacheStore.clear(App.instance) }
        }
        SettingsGroup(titleRes = R.string.pref_header_interactions) {
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
        }
        SettingsGroup(titleRes = R.string.pref_header_playlists) {
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
        }
        SettingsGroup(titleRes = R.string.pref_header_files) {
            ExternalPreference(
                titleRes = R.string.pref_title_start_directory,
                summaryRes = R.string.pref_summary_start_directory
            ) {
                val contractTool = (context as? PathSelectorRequester)?.pathSelectorContractTool
                val preference = Setting(context)[Keys.startDirectoryPath]
                contractTool?.launch(preference.data) { path ->
                    if (path != null) {
                        val file = File(path)
                        if (file.exists() && !file.isFile) preference.data = path
                    }
                }
            }
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
                onValueChanged = { newValue ->
                    if (!newValue) {
                        // clear lyrics displaying on the status bar now
                        StatusBarLyric.stopLyric()
                    }
                }
            )
            FloatPreference(
                key = Keys.coverLyricsSize,
                valueRange = 10f..28f,
                steps = 8,
                titleRes = R.string.pref_title_lyrics_size_cover,
                summaryRes = R.string.pref_summary_lyrics_size_cover,
            )
            ListPreference(
                key = Keys.coverLyricsAlign,
                optionsValues = listOf(
                    LYRICS_ALIGN_LEFT,
                    LYRICS_ALIGN_RIGHT,
                    LYRICS_ALIGN_CENTER
                ),
                optionsValuesLocalized = listOf(
                    R.string.pref_value_align_left,
                    R.string.pref_value_align_right,
                    R.string.pref_value_align_center,
                ),
                titleRes = R.string.pref_title_lyrics_align_cover,
                summaryRes = R.string.pref_summary_lyrics_align_cover,
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