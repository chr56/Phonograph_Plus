/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.setting

import player.phonograph.R
import player.phonograph.settings.Keys
import player.phonograph.ui.modules.setting.components.BooleanPreference
import player.phonograph.ui.modules.setting.components.DialogPreference
import player.phonograph.ui.modules.setting.components.SettingsGroup
import player.phonograph.ui.modules.setting.dialog.NotificationActionsConfigDialog
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
fun PreferenceScreenNotification() {
    Column(
        Modifier.verticalScroll(rememberScrollState())
    ) {
        SettingsGroup(titleRes = R.string.pref_header_notification) {
            BooleanPreference(
                key = Keys.persistentPlaybackNotification,
                titleRes = R.string.pref_title_persistent_playback_notification,
                summaryRes = R.string.pref_summary_persistent_playback_notification,
            )
            BooleanPreference(
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
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.systemBars))
    }
}