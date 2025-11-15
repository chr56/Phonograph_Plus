/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.setting.dialog

import player.phonograph.R
import player.phonograph.model.ui.NowPlayingScreenStyle
import player.phonograph.settings.Keys
import player.phonograph.settings.Preference
import player.phonograph.settings.Setting
import player.phonograph.ui.compose.components.ActionItem
import player.phonograph.ui.modules.setting.elements.NowPlayingScreenStyleSettings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class NowPlayingScreenStylePreferenceDialog : AbsSettingsDialog() {

    @Composable
    override fun Content() {
        val context = LocalContext.current

        val preference: Preference<NowPlayingScreenStyle> = remember { Setting(context)[Keys.nowPlayingScreenStyle] }
        val state: MutableStateFlow<NowPlayingScreenStyle> = remember { MutableStateFlow(preference.default) }
        LaunchedEffect(preference) { preference.flow.collect { state.emit(it) } }

        val currentConfig by state.collectAsState()

        SettingsDialog(
            modifier = Modifier,
            title = stringResource(R.string.pref_title_now_playing_screen_style),
            actions = listOf(
                ActionItem(
                    Icons.Default.Refresh,
                    textRes = R.string.action_reset,
                    onClick = {
                        lifecycleScope.launch(Dispatchers.IO) { preference.reset() }
                        dismiss()
                    }
                ),
                ActionItem(
                    Icons.Default.Check,
                    textRes = android.R.string.ok,
                    onClick = {
                        lifecycleScope.launch(Dispatchers.IO) { preference.edit { state.value } }
                        dismiss()
                    }
                ),
            )
        ) {
            Surface(
                modifier = Modifier
                    .heightIn(min = 120.dp, max = 480.dp)
                    .padding(vertical = 16.dp),
                elevation = 8.dp
            ) {
                Column(
                    Modifier
                        .padding(horizontal = 24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    NowPlayingScreenStyleSettings(currentConfig) { newConfig ->
                        state.value = newConfig
                    }
                }
            }
        }

    }

}

