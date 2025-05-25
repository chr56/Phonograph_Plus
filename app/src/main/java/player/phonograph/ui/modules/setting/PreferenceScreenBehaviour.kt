/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.setting

import player.phonograph.R
import player.phonograph.settings.Keys
import player.phonograph.ui.modules.setting.components.BooleanPreference
import player.phonograph.ui.modules.setting.components.ExternalPreference
import player.phonograph.ui.modules.setting.components.SettingsGroup
import player.phonograph.util.NavigationUtil
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import android.content.Intent
import android.content.pm.PackageManager
import android.media.audiofx.AudioEffect
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.TIRAMISU


@Composable
fun PreferenceScreenBehaviour() {
    Column(
        Modifier.verticalScroll(rememberScrollState())
    ) {
        SettingsGroup(titleRes = R.string.pref_header_audio) {
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
            EqualizerSetting()
        }
        SettingsGroup(titleRes = R.string.pref_header_player_behaviour) {
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
        }
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.systemBars))
    }
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
        titleRes = R.string.label_equalizer,
        summaryRes = if (hasEqualizer) R.string.err_no_equalizer else 0
    ) {
        if (activity != null) {
            NavigationUtil.openEqualizer(activity)
        }
    }
}