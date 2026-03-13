/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.compose.settings

import player.phonograph.ui.modules.setting.PhonographPreferenceScreen
import player.phonograph.ui.modules.setting.PreferenceScreenAdvanced
import player.phonograph.ui.modules.setting.PreferenceScreenAppearance
import player.phonograph.ui.modules.setting.PreferenceScreenBehaviour
import player.phonograph.ui.modules.setting.PreferenceScreenContent
import player.phonograph.ui.modules.setting.PreferenceScreenNotification
import player.phonograph.ui.modules.setting.SettingsViewModel
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Preview
@Composable
fun PhonographPreferenceScreenPreview() {
    PhonographPreferenceScreen(SettingsViewModel())
}


@Preview
@Composable
fun PreferenceScreenAppearancePreview() {
    PreferenceScreenAppearance()
}

@Preview
@Composable
fun PreferenceScreenContentPreview() {
    PreferenceScreenContent()
}

@Preview
@Composable
fun PreferenceScreenBehaviourPreview() {
    PreferenceScreenBehaviour()
}

@Preview
@Composable
fun PreferenceScreenNotificationPreview() {
    PreferenceScreenNotification()
}

@Preview
@Composable
fun PreferenceScreenAdvancedPreview() {
    PreferenceScreenAdvanced()
}