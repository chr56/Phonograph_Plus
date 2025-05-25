/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.setting.dialog

import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import com.vanpra.composematerialdialogs.title
import player.phonograph.R
import player.phonograph.model.ui.NowPlayingScreenStyle
import player.phonograph.settings.Keys
import player.phonograph.settings.Preference
import player.phonograph.settings.Setting
import player.phonograph.ui.compose.ComposeViewDialogFragment
import player.phonograph.ui.compose.PhonographTheme
import player.phonograph.ui.modules.setting.elements.NowPlayingScreenStyleSettings
import player.phonograph.util.theme.accentColoredButtonStyle
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class NowPlayingScreenStylePreferenceDialog : ComposeViewDialogFragment() {

    @Composable
    override fun Content() {
        val context = LocalContext.current

        val preference: Preference<NowPlayingScreenStyle> = remember { Setting(context)[Keys.nowPlayingScreenStyle] }
        val state: MutableStateFlow<NowPlayingScreenStyle> = remember { MutableStateFlow(preference.default) }
        LaunchedEffect(preference) { preference.flow.collect { state.emit(it) } }

        val currentConfig by state.collectAsState()

        PhonographTheme {
            MaterialDialog(
                dialogState = rememberMaterialDialogState(true),
                onCloseRequest = { dismiss() },
                buttons = {
                    negativeButton(
                        res = android.R.string.cancel,
                        textStyle = accentColoredButtonStyle()
                    ) { dismiss() }

                    button(
                        res = R.string.action_reset,
                        textStyle = accentColoredButtonStyle()
                    ) {
                        lifecycleScope.launch(Dispatchers.IO) { preference.reset() }
                        dismiss()
                    }

                    positiveButton(
                        res = android.R.string.ok,
                        textStyle = accentColoredButtonStyle()
                    ) {
                        lifecycleScope.launch(Dispatchers.IO) { preference.edit { state.value } }
                        dismiss()
                    }
                }
            ) {
                Spacer(Modifier.height(12.dp))
                title(res = R.string.pref_title_now_playing_screen_style)
                Column(
                    Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    NowPlayingScreenStyleSettings(currentConfig) { newConfig -> state.value = newConfig }
                }
            }
        }
    }

}

