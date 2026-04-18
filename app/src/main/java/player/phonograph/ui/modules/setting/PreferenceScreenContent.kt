/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.setting

import player.phonograph.App
import player.phonograph.R
import player.phonograph.coil.cache.CacheStore
import player.phonograph.model.repo.PROVIDER_INTERNAL_DATABASE
import player.phonograph.model.repo.PROVIDER_MEDIASTORE_DIRECT
import player.phonograph.model.repo.SYNC_MODE_EXCLUDE_GENRES
import player.phonograph.model.repo.SYNC_MODE_STANDARD
import player.phonograph.model.time.Duration
import player.phonograph.model.time.TimeIntervalCalculationMode
import player.phonograph.model.time.displayText
import player.phonograph.repo.loader.Albums
import player.phonograph.repo.loader.Artists
import player.phonograph.repo.loader.Genres
import player.phonograph.repo.loader.Songs
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.ui.modules.explorer.PathSelectorRequester
import player.phonograph.ui.modules.setting.components.BooleanPreference
import player.phonograph.ui.modules.setting.components.DialogPreference
import player.phonograph.ui.modules.setting.components.ExternalPreference
import player.phonograph.ui.modules.setting.components.ListPreference
import player.phonograph.ui.modules.setting.components.SettingsGroup
import player.phonograph.ui.modules.setting.dialog.ImageSourceConfigDialog
import player.phonograph.ui.modules.setting.dialog.LastAddedPlaylistIntervalDialog
import player.phonograph.ui.modules.setting.dialog.PathFilterEditorDialog
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
import java.io.File

@Composable
fun PreferenceScreenContent() {
    val context = LocalContext.current
    Column(
        Modifier.verticalScroll(rememberScrollState())
    ) {
        SettingsGroup(titleRes = R.string.pref_header_path_filter) {
            BooleanPreference(
                key = Keys.pathFilterExcludeMode,
                titleRes = R.string.path_filter,
                currentValueForHint = { context, mode ->
                    context.getString(if (mode) R.string.path_filter_excluded_mode else R.string.path_filter_included_mode)
                }
            )
            DialogPreference(
                dialog = PathFilterEditorDialog.ExcludedMode::class.java,
                titleRes = R.string.label_excluded_paths,
                summaryRes = R.string.pref_summary_path_filter_excluded_mode,
                enabled = dependOn(Keys.pathFilterExcludeMode) { it == true }
            )
            DialogPreference(
                dialog = PathFilterEditorDialog.IncludedMode::class.java,
                titleRes = R.string.label_included_paths,
                summaryRes = R.string.pref_summary_path_filter_included_mode,
                enabled = dependOn(Keys.pathFilterExcludeMode) { it == false }
            )
        }
        SettingsGroup(titleRes = R.string.pref_header_library) {
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
        }
        SettingsGroup(titleRes = R.string.pref_header_images) {
            DialogPreference(
                dialog = ImageSourceConfigDialog::class.java,
                titleRes = R.string.image_source_config,
                reset = {
                    resetPreference(it, R.string.image_source_config, Keys.imageSourceConfig)
                }
            )
            BooleanPreference(
                key = Keys.imageCache,
                summaryRes = R.string.pref_summary_image_cache,
                titleRes = R.string.pref_title_image_cache,
            )
            ExternalPreference(titleRes = R.string.action_clear_image_cache) { CacheStore.clear(App.instance) }
        }
        SettingsGroup(titleRes = R.string.pref_header_playlists) {
            DialogPreference(
                dialog = LastAddedPlaylistIntervalDialog::class.java,
                titleRes = R.string.pref_title_last_added_interval,
                currentValueForHint = { context ->
                    val resources = context.resources
                    val setting = Setting(context)
                    val calculationMode =
                        setting[Keys._lastAddedCutOffMode].data
                            .let(TimeIntervalCalculationMode.Companion::from)
                    val duration =
                        setting[Keys._lastAddedCutOffDuration].data
                            .let(Duration.Companion::from)
                    if (calculationMode != null && duration != null)
                        resources.getString(
                            R.string.time_interval_text,
                            calculationMode.displayText(resources),
                            duration.value,
                            duration.unit.displayText(resources)
                        )
                    else
                        resources.getString(R.string._default)
                },
                reset = {
                    resetPreference(
                        it,
                        R.string.pref_title_last_added_interval,
                        Keys._lastAddedCutOffMode,
                        Keys._lastAddedCutOffDuration
                    )
                }
            )
        }
        SettingsGroup(titleRes = R.string.pref_header_files) {
            ExternalPreference(
                titleRes = R.string.pref_title_start_directory,
                summaryRes = R.string.pref_summary_start_directory
            ) {
                val contractTool = (context as? PathSelectorRequester)?.pathSelectorContractTool
                val preference = Setting(context)[Keys.startDirectoryPath]
                contractTool?.launch(preference.data) { path ->
                    if (path != null) {
                        val file = File(path)
                        if (file.exists() && !file.isFile) preference.data = path
                    }
                }
            }
        }
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.systemBars))
    }
}