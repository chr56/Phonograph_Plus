/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.setting

import player.phonograph.R
import player.phonograph.repo.loader.replaceFavoriteSongDelegate
import player.phonograph.settings.Keys
import player.phonograph.ui.modules.setting.components.BooleanPreference
import player.phonograph.ui.modules.setting.components.SettingsGroup
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

@Composable
fun PreferenceScreenAdvanced() {
    Column(
        Modifier.verticalScroll(rememberScrollState())
    ) {
        val context = LocalContext.current
        SettingsGroup(titleRes = R.string.pref_header_compatibility) {
            BooleanPreference(
                key = Keys.alwaysUseMediaSessionToDisplayCover,
                titleRes = R.string.pref_title_always_use_media_session_to_display_cover,
                summaryRes = R.string.pref_summary_always_use_media_session_to_display_cover,
            )
            BooleanPreference(
                key = Keys.useLegacyFavoritePlaylistImpl,
                titleRes = R.string.pref_title_use_legacy_favorite_playlist_impl,
                summaryRes = R.string.pref_summary_use_legacy_favorite_playlist_impl,
            ) {
                replaceFavoriteSongDelegate(context)
            }
            BooleanPreference(
                key = Keys.useLegacyListFilesImpl,
                titleRes = R.string.option_use_legacy_list_Files,
            )
            BooleanPreference(
                key = Keys.useLegacyStatusBarLyricsApi,
                titleRes = R.string.pref_title_use_legacy_status_bar_lyrics_api,
                summaryRes = R.string.pref_summary_use_legacy_status_bar_lyrics_api,
            )
            BooleanPreference(
                key = Keys.disableRealTimeSearch,
                titleRes = R.string.pref_title_disable_real_time_search,
                summaryRes = R.string.pref_summary_disable_real_time_search,
            )
        }
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.systemBars))
    }
}
