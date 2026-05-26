/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.setting

import player.phonograph.R
import player.phonograph.model.time.TimeIntervalCalculationMode
import player.phonograph.settings.Keys
import player.phonograph.settings.Settings
import player.phonograph.ui.modules.setting.components.BooleanPreference
import player.phonograph.ui.modules.setting.components.DialogPreference
import player.phonograph.ui.modules.setting.components.SettingsGroup
import player.phonograph.ui.modules.setting.dialog.CheckUpdateIntervalDialog
import player.phonograph.ui.resource.Texts
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
fun PreferenceScreenUpdates() {
    Column(
        Modifier.verticalScroll(rememberScrollState())
    ) {
        SettingsGroup(titleRes = R.string.action_check_for_updates) {
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
                },
                currentValueForHint = {
                    val resources = it.resources
                    val preference = Settings(it)[Keys.checkUpdateInterval]
                    val duration = preference.data
                    Texts.duration(resources, duration, TimeIntervalCalculationMode.EVERY)
                }
            )
        }
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.systemBars))
    }
}
