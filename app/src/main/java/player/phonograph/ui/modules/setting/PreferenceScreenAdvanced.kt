/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.setting

import player.phonograph.R
import player.phonograph.model.repo.PROVIDER_MEDIASTORE_DIRECT
import player.phonograph.model.repo.PROVIDER_INTERNAL_DATABASE
import player.phonograph.model.repo.SYNC_MODE_EXCLUDE_GENRES
import player.phonograph.model.repo.SYNC_MODE_STANDARD
import player.phonograph.repo.loader.Albums
import player.phonograph.repo.loader.Artists
import player.phonograph.repo.loader.FavoriteSongs
import player.phonograph.repo.loader.Genres
import player.phonograph.repo.loader.Songs
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.ui.compose.ExperimentalContentThemeOverride
import player.phonograph.ui.modules.setting.components.BooleanPreference
import player.phonograph.ui.modules.setting.components.DialogPreference
import player.phonograph.ui.modules.setting.components.ListPreference
import player.phonograph.ui.modules.setting.components.SettingsGroup
import player.phonograph.ui.modules.setting.dialog.TagSeparatorsEditorDialog
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
import androidx.compose.ui.res.stringResource

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
                onValueChanged = { FavoriteSongs.recreate(context) }
            )
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
        SettingsGroup(titleRes = R.string.pref_header_experimental) {
            ExperimentalContentThemeOverride {
                ListPreference(
                    key = Keys.musicLibrarySource,
                    optionsValues = listOf(
                        PROVIDER_MEDIASTORE_DIRECT,
                        PROVIDER_INTERNAL_DATABASE,
                    ),
                    optionsValuesLocalized = listOf(
                        R.string.music_library_metadata_source_mediastore,
                        R.string.music_library_metadata_source_database,
                    ),
                    title = stringResource(R.string.music_library_metadata_source),
                ) { _, _ ->
                    Songs.recreate(context) && Albums.recreate(context) &&
                            Artists.recreate(context) && Genres.recreate(context)
                }
                ListPreference(
                    key = Keys.musicLibrarySyncMode,
                    optionsValues = listOf(
                        SYNC_MODE_STANDARD,
                        SYNC_MODE_EXCLUDE_GENRES,
                    ),
                    optionsValuesLocalized = listOf(
                        R.string.music_library_metadata_sync_mode_standard,
                        R.string.music_library_metadata_sync_mode_exclude_genres,
                    ),
                    title = stringResource(R.string.music_library_metadata_sync_mode),
                )
                DialogPreference(
                    dialog = TagSeparatorsEditorDialog.ArtistsSeparatorsEditor::class.java,
                    title = stringResource(R.string.pref_title_music_tags_artists_separators),
                    currentValueForHint = { context ->
                        Setting(context)[Keys.tagSeparatorsArtists].read().joinToString(" ")
                    }
                )
                DialogPreference(
                    dialog = TagSeparatorsEditorDialog.FeaturesArtistsAbbrEditor::class.java,
                    title = stringResource(R.string.pref_title_music_tags_featuring_artists_abbr),
                    currentValueForHint = { context ->
                        Setting(context)[Keys.tagAbbrFeatureArtists].read().joinToString(" ")
                    }
                )
                DialogPreference(
                    dialog = TagSeparatorsEditorDialog.GenreSeparatorsEditor::class.java,
                    title = stringResource(R.string.pref_title_music_tags_genres_separators),
                    currentValueForHint = { context ->
                        Setting(context)[Keys.tagSeparatorsGenres].read().joinToString(" ")
                    }
                )
            }
        }
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.systemBars))
    }
}
