/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.ui.modules.setting

import player.phonograph.R
import player.phonograph.mechanism.StatusBarLyric
import player.phonograph.model.lyrics.LYRICS_ALIGN_CENTER
import player.phonograph.model.lyrics.LYRICS_ALIGN_LEFT
import player.phonograph.model.lyrics.LYRICS_ALIGN_RIGHT
import player.phonograph.settings.Keys
import player.phonograph.ui.modules.setting.components.BooleanPreference
import player.phonograph.ui.modules.setting.components.FloatPreference
import player.phonograph.ui.modules.setting.components.ListPreference
import player.phonograph.ui.modules.setting.components.SettingsGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun PreferenceScreenLyrics() {
    val context = LocalContext.current
    Column(
        Modifier.verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(16.dp))
        BooleanPreference(
            key = Keys.enableLyrics,
            titleRes = R.string.pref_title_load_lyrics,
            summaryRes = R.string.pref_summary_load_lyrics,
        )
        SettingsGroup(titleRes = R.string.pref_header_lyrics) {
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
            BooleanPreference(
                key = Keys.broadcastSynchronizedLyrics,
                titleRes = R.string.pref_title_send_lyrics,
                summaryRes = R.string.pref_summary_send_lyrics,
            )
        }
        SettingsGroup(titleRes = R.string.pref_header_appearance) {
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
            FloatPreference(
                key = Keys.dialogLyricsSize,
                valueRange = 8f..26f,
                steps = 8,
                titleRes = R.string.pref_title_lyrics_size_dialog,
                summaryRes = R.string.pref_summary_lyrics_size_dialog,
            )
            BooleanPreference(
                key = Keys.displaySynchronizedLyricsTimeAxis,
                titleRes = R.string.pref_title_display_lyrics_time_axis,
                summaryRes = R.string.pref_summary_display_lyrics_time_axis,
            )
        }
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.systemBars))
    }
}