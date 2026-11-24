/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.setting.dialog

import player.phonograph.R
import player.phonograph.model.menu.ActionEntry
import player.phonograph.model.ui.NowPlayingScreenStyle
import player.phonograph.settings.Keys
import player.phonograph.settings.Preference
import player.phonograph.settings.Settings
import player.phonograph.ui.modules.setting.elements.NowPlayingScreenStyleSettings
import player.phonograph.ui.resource.ActionSemantics
import androidx.compose.foundation.layout.padding
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

        val preference: Preference<NowPlayingScreenStyle> = remember { Settings(context)[Keys.nowPlayingScreenStyle] }
        val state: MutableStateFlow<NowPlayingScreenStyle> = remember { MutableStateFlow(preference.default) }
        LaunchedEffect(preference) { preference.flow.collect { state.emit(it) } }

        val currentConfig by state.collectAsState()

        SettingsDialog(
            modifier = Modifier,
            title = stringResource(R.string.pref_title_now_playing_screen_style),
            actions = listOf(
                ActionEntry.ToolbarEntry(
                    semantic = ActionSemantics.Reset,
                    action = {
                        lifecycleScope.launch(Dispatchers.IO) { preference.reset() }
                        dismiss()
                    }
                ),
                ActionEntry.ToolbarEntry(
                    semantic = ActionSemantics.Confirm,
                    action = {
                        lifecycleScope.launch(Dispatchers.IO) { preference.edit { state.value } }
                        dismiss()
                    }
                ),
            ),
            scrollable = true,
            innerShadow = true,
        ) {
            NowPlayingScreenStyleSettings(
                current = currentConfig,
                update = { newConfig -> state.value = newConfig },
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

    }

}

