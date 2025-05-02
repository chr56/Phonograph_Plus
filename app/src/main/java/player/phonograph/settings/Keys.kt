/*
 *  Copyright (c) 2022~2023 chr_56
 */

@file:Suppress("ObjectPropertyName")

package player.phonograph.settings

import lib.phonograph.misc.MonetColor
import player.phonograph.model.SongClickMode
import player.phonograph.model.coil.ImageSourceConfig
import player.phonograph.model.file.defaultStartDirectory
import player.phonograph.model.notification.NotificationActionsConfig
import player.phonograph.model.pages.PagesConfig
import player.phonograph.model.sort.SortMode
import player.phonograph.model.sort.SortRef
import player.phonograph.model.time.Duration
import player.phonograph.model.time.TimeIntervalCalculationMode
import player.phonograph.model.ui.GeneralTheme
import player.phonograph.model.ui.ItemLayoutStyle
import player.phonograph.model.ui.NowPlayingScreenStyle
import util.theme.materials.MaterialColor
import java.io.File
import androidx.datastore.preferences.core.booleanPreferencesKey as booleanPK
import androidx.datastore.preferences.core.intPreferencesKey as intPK
import androidx.datastore.preferences.core.longPreferencesKey as longPK
import androidx.datastore.preferences.core.stringPreferencesKey as stringPK

/**
 * All available [PreferenceKey] registered
 */
object Keys {

    //<editor-fold desc="Appearance">

    val theme
        get() = PrimitiveKey<String>(stringPK("theme")) { GeneralTheme.THEME_AUTO_LIGHTBLACK }

    //<editor-fold desc="Colors">
    val selectedPrimaryColor
        get() = PrimitiveKey<Int>(intPK("primary_color_selected")) { MaterialColor.Blue._A400.asColor }
    val selectedAccentColor
        get() = PrimitiveKey<Int>(intPK("accent_color_selected")) { MaterialColor.Yellow._900.asColor }
    val enableMonet
        get() = PrimitiveKey<Boolean>(booleanPK("enable_monet")) { false }
    val monetPalettePrimaryColor
        get() = PrimitiveKey<Int>(intPK("primary_color_monet_palette")) { MonetColor.defaultMonetPrimaryColor.value }
    val monetPaletteAccentColor
        get() = PrimitiveKey<Int>(intPK("accent_color_monet_palette")) { MonetColor.defaultMonetAccentColor.value }
    val coloredAppShortcuts
        get() = PrimitiveKey<Boolean>(booleanPK("colored_app_shortcuts")) { true }
    //</editor-fold>

    //<editor-fold desc="Now Playing">
    val _nowPlayingScreenStyle
        get() = PrimitiveKey<String>(stringPK("now_playing_screen_style")) { "" }
    val nowPlayingScreenStyle
        get() = CompositeKey<NowPlayingScreenStyle>(NowPlayingScreenStylePreferenceProvider)
    //</editor-fold>

    //<editor-fold desc="Library">
    val _homeTabConfigJson
        get() = PrimitiveKey<String>(stringPK("home_tab_config")) { "" }
    val homeTabConfig
        get() = CompositeKey<PagesConfig>(HomeTabConfigPreferenceProvider)
    val rememberLastTab
        get() = PrimitiveKey<Boolean>(booleanPK("remember_last_tab")) { true }
    val lastPage
        get() = PrimitiveKey<Int>(intPK("last_start_page")) { 0 } // HIDDEN FROM UI
    val fixedTabLayout
        get() = PrimitiveKey<Boolean>(booleanPK("fixed_tab_layout")) { false }
    //</editor-fold>

    //</editor-fold>

    //<editor-fold desc="Content">

    //<editor-fold desc="PathFilter">
    val pathFilterExcludeMode
        get() = PrimitiveKey<Boolean>(booleanPK("path_filter_exclude_mode")) { true }
    //</editor-fold>

    //<editor-fold desc="Images">
    val _imageSourceConfigJson
        get() = PrimitiveKey<String>(stringPK("image_source_config")) { "{}" }
    val imageSourceConfig
        get() = CompositeKey<ImageSourceConfig>(CoilImageSourcePreferenceProvider)
    val imageCache
        get() = PrimitiveKey<Boolean>(booleanPK("image_cache")) { false }
    //</editor-fold>

    //<editor-fold desc="Interactions">
    val songItemClickMode
        get() = PrimitiveKey<Int>(intPK("song_item_click_extra_flag")) { SongClickMode.SONG_PLAY_NOW }
    val songItemClickExtraFlag
        get() = PrimitiveKey<Int>(intPK("song_item_click_extra_mode")) { SongClickMode.FLAG_MASK_PLAY_QUEUE_IF_EMPTY }
    val externalPlayRequestShowPrompt
        get() = PrimitiveKey<Boolean>(booleanPK("external_play_request_show_prompt")) { false }
    val externalPlayRequestSingleMode
        get() = PrimitiveKey<Int>(intPK("external_play_request_single_mode")) { SongClickMode.SONG_PLAY_NOW }
    val externalPlayRequestMultipleMode
        get() = PrimitiveKey<Int>(intPK("external_play_request_multiple_mode")) { SongClickMode.QUEUE_PLAY_NOW }
    val externalPlayRequestSilence
        get() = PrimitiveKey<Boolean>(booleanPK("external_play_request_silence")) { false }
    //</editor-fold>

    //<editor-fold desc="Playlist">
    val _lastAddedCutOffMode
        get() = PrimitiveKey<Int>(intPK("last_added_cutoff_mode")) { TimeIntervalCalculationMode.PAST.value }

    val _lastAddedCutOffDuration
        get() = PrimitiveKey<String>(stringPK("last_added_cutoff_duration")) { Duration.Week(3).serialise() }

    val lastAddedCutoffTimeStamp
        get() = CompositeKey<Long>(LastAddedCutOffDurationPreferenceProvider)
    //</editor-fold>

    //<editor-fold desc="Files">
    val _startDirectoryPath
        get() = PrimitiveKey<String>(stringPK("start_directory")) { defaultStartDirectory.path }
    val startDirectory
        get() = CompositeKey<File>(StartDirectoryPreferenceProvider)
    //</editor-fold>

    //<editor-fold desc="Lyrics">
    val enableLyrics
        get() = PrimitiveKey<Boolean>(booleanPK("enable_lyrics")) { true }
    val synchronizedLyricsShow
        get() = PrimitiveKey<Boolean>(booleanPK("synchronized_lyrics_show")) { true }
    val displaySynchronizedLyricsTimeAxis
        get() = PrimitiveKey<Boolean>(booleanPK("display_lyrics_time_axis")) { true }
    val broadcastSynchronizedLyrics
        get() = PrimitiveKey<Boolean>(booleanPK("synchronized_lyrics_send")) { true }
    //</editor-fold>

    //</editor-fold>

    //<editor-fold desc="Behaviour">

    //<editor-fold desc="Audio">
    val audioDucking
        get() = PrimitiveKey<Boolean>(booleanPK("audio_ducking")) { true }
    val resumeAfterAudioFocusGain
        get() = PrimitiveKey<Boolean>(booleanPK("resume_after_audio_focus_gain")) { false }
    val alwaysPlay
        get() = PrimitiveKey<Boolean>(booleanPK("always_play")) { false }
    //</editor-fold>

    //<editor-fold desc="Player Behaviour">
    val gaplessPlayback
        get() = PrimitiveKey<Boolean>(booleanPK("gapless_playback")) { false }
    val broadcastCurrentPlayerState
        get() = PrimitiveKey<Boolean>(booleanPK("broadcast_current_player_state")) { true }
    //</editor-fold>

    //</editor-fold>

    //<editor-fold desc="Notification">
    val persistentPlaybackNotification
        get() = PrimitiveKey<Boolean>(booleanPK("persistent_playback_notification")) { false }
    val classicNotification
        get() = PrimitiveKey<Boolean>(booleanPK("classic_notification")) { false }
    val coloredNotification
        get() = PrimitiveKey<Boolean>(booleanPK("colored_notification")) { true }
    val _notificationActionsJson
        get() = PrimitiveKey<String>(stringPK("notification_actions")) { "{}" }
    val notificationActions
        get() = CompositeKey<NotificationActionsConfig>(NotificationActionsPreferenceProvider)
    //</editor-fold>

    //<editor-fold desc="Advanced">

    //<editor-fold desc="Compatibility">
    val alwaysUseMediaSessionToDisplayCover
        get() = PrimitiveKey<Boolean>(booleanPK("always_use_media_session_to_display_cover")) { false }
    val useLegacyFavoritePlaylistImpl
        get() = PrimitiveKey<Boolean>(booleanPK("use_legacy_favorite_playlist_impl")) { false }
    val useLegacyListFilesImpl
        get() = PrimitiveKey<Boolean>(booleanPK("use_legacy_list_files_impl")) { false }
    val useLegacyStatusBarLyricsApi
        get() = PrimitiveKey<Boolean>(booleanPK("use_legacy_status_bar_lyrics_api")) { false }
    val disableRealTimeSearch
        get() = PrimitiveKey<Boolean>(booleanPK("disable_real_time_search")) { false }
    //</editor-fold>

    //</editor-fold>

    //<editor-fold desc="Updates">
    val checkUpgradeAtStartup
        get() = PrimitiveKey<Boolean>(booleanPK("check_upgrade_at_startup")) { false }
    val _checkUpdateInterval
        get() = PrimitiveKey<String>(stringPK("check_upgrade_interval")) { Duration.Day(1).serialise() }
    val checkUpdateInterval
        get() = CompositeKey<Duration>(CheckUpdateIntervalPreferenceProvider)
    val lastCheckUpgradeTimeStamp
        get() = PrimitiveKey<Long>(longPK("last_check_upgrade_time")) { 0 } // HIDDEN FROM UI
    val ignoreUpgradeDate
        get() = PrimitiveKey<Long>(longPK("ignore_upgrade_date")) { 0 } // HIDDEN FROM UI
    //</editor-fold>

    //<editor-fold desc="SleepTimer (HIDDEN FROM UI)">
    val lastSleepTimerValue
        get() = PrimitiveKey<Int>(intPK("last_sleep_timer_value")) { 30 }
    val nextSleepTimerElapsedRealTime
        get() = PrimitiveKey<Long>(longPK("next_sleep_timer_elapsed_real_time")) { -1L }
    val sleepTimerFinishMusic
        get() = PrimitiveKey<Boolean>(booleanPK("sleep_timer_finish_song")) { false }
    //</editor-fold>

    //<editor-fold desc="Display Settings (HIDDEN FROM UI)">

    //<editor-fold desc="SortMode">
    val _songSortMode
        get() = PrimitiveKey<String>(stringPK("song_sort_mode")) { SortMode(SortRef.ID, false).serialize() }
    val songSortMode
        get() = CompositeKey<SortMode>(SortModePreferenceProvider.SongSortMode)
    val _albumSortMode
        get() = PrimitiveKey<String>(stringPK("album_sort_mode")) { SortMode(SortRef.ID, false).serialize() }
    val albumSortMode
        get() = CompositeKey<SortMode>(SortModePreferenceProvider.AlbumSortMode)
    val _artistSortMode
        get() = PrimitiveKey<String>(stringPK("artist_sort_mode")) { SortMode(SortRef.ID, false).serialize() }
    val artistSortMode
        get() = CompositeKey<SortMode>(SortModePreferenceProvider.ArtistSortMode)
    val _genreSortMode
        get() = PrimitiveKey<String>(stringPK("genre_sort_mode")) { SortMode(SortRef.ID, false).serialize() }
    val genreSortMode
        get() = CompositeKey<SortMode>(SortModePreferenceProvider.GenreSortMode)
    val _fileSortMode
        get() = PrimitiveKey<String>(stringPK("file_sort_mode")) { SortMode(SortRef.ID, false).serialize() }
    val fileSortMode
        get() = CompositeKey<SortMode>(SortModePreferenceProvider.FileSortMode)
    val _collectionSortMode
        get() = PrimitiveKey<String>(stringPK("song_collection_sort_mode")) { SortMode(SortRef.ID, false).serialize() }
    val collectionSortMode
        get() = CompositeKey<SortMode>(SortModePreferenceProvider.CollectionSortMode)
    val _playlistSortMode
        get() = PrimitiveKey<String>(stringPK("playlist_sort_mode")) { SortMode(SortRef.ID, false).serialize() }
    val playlistSortMode
        get() = CompositeKey<SortMode>(SortModePreferenceProvider.PlaylistSortMode)
    //</editor-fold>

    //<editor-fold desc="ColoredFooters">
    val albumArtistColoredFooters
        get() = PrimitiveKey<Boolean>(booleanPK("album_artist_colored_footers")) { true }
    val albumColoredFooters
        get() = PrimitiveKey<Boolean>(booleanPK("album_colored_footers")) { true }
    val songColoredFooters
        get() = PrimitiveKey<Boolean>(booleanPK("song_colored_footers")) { true }
    val artistColoredFooters
        get() = PrimitiveKey<Boolean>(booleanPK("artist_colored_footers")) { true }
    //</editor-fold>

    //<editor-fold desc="File Related">
    val showFileImages
        get() = PrimitiveKey<Boolean>(booleanPK("show_file_imagines")) { false }
    //</editor-fold>

    //<editor-fold desc="Layout">
    val _songItemLayout
        get() = PrimitiveKey<Int>(intPK("song_item_layout")) { ItemLayoutStyle.LIST_EXTENDED.ordinal }
    val songItemLayout
        get() = CompositeKey<ItemLayoutStyle>(ItemLayoutProvider.SongItemLayoutProvider)
    val _songItemLayoutLand
        get() = PrimitiveKey<Int>(intPK("song_item_layout_land")) { ItemLayoutStyle.LIST.ordinal }
    val songItemLayoutLand
        get() = CompositeKey<ItemLayoutStyle>(ItemLayoutProvider.LandSongItemLayoutProvider)

    val _albumItemLayout
        get() = PrimitiveKey<Int>(intPK("album_item_layout")) { ItemLayoutStyle.LIST_3L.ordinal }
    val albumItemLayout
        get() = CompositeKey<ItemLayoutStyle>(ItemLayoutProvider.AlbumItemLayoutProvider)
    val _albumItemLayoutLand
        get() = PrimitiveKey<Int>(intPK("album_item_layout_land")) { ItemLayoutStyle.LIST_3L.ordinal }
    val albumItemLayoutLand
        get() = CompositeKey<ItemLayoutStyle>(ItemLayoutProvider.LandAlbumItemLayoutProvider)

    val _artistItemLayout
        get() = PrimitiveKey<Int>(intPK("artist_item_layout")) { ItemLayoutStyle.LIST.ordinal }
    val artistItemLayout
        get() = CompositeKey<ItemLayoutStyle>(ItemLayoutProvider.ArtistItemLayoutProvider)
    val _artistItemLayoutLand
        get() = PrimitiveKey<Int>(intPK("artist_item_layout_land")) { ItemLayoutStyle.LIST_3L.ordinal }
    val artistItemLayoutLand
        get() = CompositeKey<ItemLayoutStyle>(ItemLayoutProvider.LandArtistItemLayoutProvider)

    val _folderItemLayout
        get() = PrimitiveKey<Int>(intPK("folder_item_layout")) { ItemLayoutStyle.LIST.ordinal }
    val folderItemLayout
        get() = CompositeKey<ItemLayoutStyle>(ItemLayoutProvider.FolderItemLayoutProvider)
    val _folderItemLayoutLand
        get() = PrimitiveKey<Int>(intPK("folder_item_layout_land")) { ItemLayoutStyle.LIST_3L.ordinal }
    val folderItemLayoutLand
        get() = CompositeKey<ItemLayoutStyle>(ItemLayoutProvider.LandFolderItemLayoutProvider)
    //</editor-fold>

    //<editor-fold desc="Grid Size">
    val songGridSize
        get() = PrimitiveKey<Int>(intPK("song_grid_size")) { 1 }
    val songGridSizeLand
        get() = PrimitiveKey<Int>(intPK("song_grid_size_land")) { 2 }
    val albumGridSize
        get() = PrimitiveKey<Int>(intPK("album_grid_size")) { 2 }
    val albumGridSizeLand
        get() = PrimitiveKey<Int>(intPK("album_grid_size_land")) { 3 }
    val artistGridSize
        get() = PrimitiveKey<Int>(intPK("artist_grid_size")) { 3 }
    val artistGridSizeLand
        get() = PrimitiveKey<Int>(intPK("artist_grid_size_land")) { 4 }
    val genreGridSize
        get() = PrimitiveKey<Int>(intPK("genre_grid_size")) { 1 }
    val genreGridSizeLand
        get() = PrimitiveKey<Int>(intPK("genre_grid_size_land")) { 2 }
    val playlistGridSize
        get() = PrimitiveKey<Int>(intPK("playlist_grid_size")) { 1 }
    val playlistGridSizeLand
        get() = PrimitiveKey<Int>(intPK("playlist_grid_size_land")) { 2 }
    val folderGridSize
        get() = PrimitiveKey<Int>(intPK("folder_grid_size")) { 1 }
    val folderGridSizeLand
        get() = PrimitiveKey<Int>(intPK("folder_grid_size_land")) { 2 }
    //</editor-fold>

    //</editor-fold>
}