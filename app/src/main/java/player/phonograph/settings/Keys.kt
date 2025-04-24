/*
 *  Copyright (c) 2022~2023 chr_56
 */

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
@Suppress("ObjectPropertyName")
object Keys {

    // Appearance
    val theme
        get() = PrimitiveKey<String>(stringPK(THEME)) { THEME_AUTO_LIGHTBLACK }

    val _homeTabConfigJson
        get() = PrimitiveKey<String>(stringPK(HOME_TAB_CONFIG)) { "" }

    val homeTabConfig
        get() = CompositeKey<PagesConfig>(HomeTabConfigPreferenceProvider)

    val coloredAppShortcuts
        get() = PrimitiveKey<Boolean>(booleanPK(COLORED_APP_SHORTCUTS)) { true }

    val fixedTabLayout
        get() = PrimitiveKey<Boolean>(booleanPK(FIXED_TAB_LAYOUT)) { false }

    val selectedPrimaryColor
        get() = PrimitiveKey<Int>(intPK(SELECTED_PRIMARY_COLOR)) { MaterialColor.Blue._A400.asColor }

    val selectedAccentColor
        get() = PrimitiveKey<Int>(intPK(SELECTED_ACCENT_COLOR)) { MaterialColor.Yellow._900.asColor }

    val enableMonet
        get() = PrimitiveKey<Boolean>(booleanPK(ENABLE_MONET)) { false }

    val monetPalettePrimaryColor
        get() = PrimitiveKey<Int>(intPK(MONET_PALETTE_PRIMARY_COLOR)) { MonetColor.defaultMonetPrimaryColor.value }

    val monetPaletteAccentColor
        get() = PrimitiveKey<Int>(intPK(MONET_PALETTE_ACCENT_COLOR)) { MonetColor.defaultMonetAccentColor.value }


    // Appearance - Notification
    val coloredNotification
        get() = PrimitiveKey<Boolean>(booleanPK(COLORED_NOTIFICATION)) { true }

    val classicNotification
        get() = PrimitiveKey<Boolean>(booleanPK(CLASSIC_NOTIFICATION)) { false }

    val _notificationActionsJson
        get() = PrimitiveKey<String>(stringPK(NOTIFICATION_ACTIONS)) { "{}" }

    val notificationActions
        get() = CompositeKey<NotificationActionsConfig>(NotificationActionsPreferenceProvider)

    // Behavior-Retention
    val rememberLastTab
        get() = PrimitiveKey<Boolean>(booleanPK(REMEMBER_LAST_TAB)) { true }

    val lastPage
        get() = PrimitiveKey<Int>(intPK(LAST_PAGE)) { 0 }

    val _nowPlayingScreenStyle
        get() = PrimitiveKey<String>(stringPK(NOW_PLAYING_SCREEN_STYLE)) { "" }

    val nowPlayingScreenStyle
        get() = CompositeKey<NowPlayingScreenStyle>(NowPlayingScreenStylePreferenceProvider)

    // Database

    // Behavior-File
    val _startDirectoryPath
        get() = PrimitiveKey<String>(stringPK(START_DIRECTORY)) { defaultStartDirectory.path }

    val startDirectory
        get() = CompositeKey<File>(StartDirectoryPreferenceProvider)

    val preloadImages
        get() = PrimitiveKey<Boolean>(booleanPK(PRELOAD_IMAGES)) { true }

    val _imageSourceConfigJson
        get() = PrimitiveKey<String>(stringPK(IMAGE_SOURCE_CONFIG)) { "{}" }

    val imageSourceConfig
        get() = CompositeKey<ImageSourceConfig>(CoilImageSourcePreferenceProvider)

    val imageCache
        get() = PrimitiveKey<Boolean>(booleanPK(IMAGE_CACHE)) { false }

    // Behavior-Playing
    val songItemClickMode
        get() = PrimitiveKey<Int>(intPK(SONG_ITEM_CLICK_MODE)) { SongClickMode.SONG_PLAY_NOW }

    val songItemClickExtraFlag
        get() = PrimitiveKey<Int>(intPK(SONG_ITEM_CLICK_EXTRA_FLAG)) { SongClickMode.FLAG_MASK_PLAY_QUEUE_IF_EMPTY }

    val externalPlayRequestShowPrompt
        get() = PrimitiveKey<Boolean>(booleanPK(EXTERNAL_PLAY_REQUEST_SHOW_PROMPT)) { false }

    val externalPlayRequestSingleMode
        get() = PrimitiveKey<Int>(intPK(EXTERNAL_PLAY_REQUEST_SINGLE_MODE)) { SongClickMode.SONG_PLAY_NOW }

    val externalPlayRequestMultipleMode
        get() = PrimitiveKey<Int>(intPK(EXTERNAL_PLAY_REQUEST_MULTIPLE_MODE)) { SongClickMode.QUEUE_PLAY_NOW }

    val externalPlayRequestSilence
        get() = PrimitiveKey<Boolean>(booleanPK(EXTERNAL_PLAY_REQUEST_SILENCE)) { false }

    val gaplessPlayback
        get() = PrimitiveKey<Boolean>(booleanPK(GAPLESS_PLAYBACK)) { false }

    val audioDucking
        get() = PrimitiveKey<Boolean>(booleanPK(AUDIO_DUCKING)) { true }

    val resumeAfterAudioFocusGain
        get() = PrimitiveKey<Boolean>(booleanPK(RESUME_AFTER_AUDIO_FOCUS_GAIN)) { false }

    val alwaysPlay
        get() = PrimitiveKey<Boolean>(booleanPK(ALWAYS_PLAY)) { false }

    val enableLyrics
        get() = PrimitiveKey<Boolean>(booleanPK(ENABLE_LYRICS)) { true }

    val broadcastSynchronizedLyrics
        get() = PrimitiveKey<Boolean>(booleanPK(BROADCAST_SYNCHRONIZED_LYRICS)) { true }

    val useLegacyStatusBarLyricsApi
        get() = PrimitiveKey<Boolean>(booleanPK(USE_LEGACY_STATUS_BAR_LYRICS_API)) { false }

    val broadcastCurrentPlayerState
        get() = PrimitiveKey<Boolean>(booleanPK(BROADCAST_CURRENT_PLAYER_STATE)) { true }

    val persistentPlaybackNotification
        get() = PrimitiveKey<Boolean>(booleanPK(PERSISTENT_PLAYBACK_NOTIFICATION)) { false }

    // Behavior-Lyrics
    val synchronizedLyricsShow
        get() = PrimitiveKey<Boolean>(booleanPK(SYNCHRONIZED_LYRICS_SHOW)) { true }

    val displaySynchronizedLyricsTimeAxis
        get() = PrimitiveKey<Boolean>(booleanPK(DISPLAY_LYRICS_TIME_AXIS)) { true }

    val _lastAddedCutOffMode
        get() = PrimitiveKey<Int>(intPK(LAST_ADDED_CUTOFF_MODE)) { TimeIntervalCalculationMode.PAST.value }

    val _lastAddedCutOffDuration
        get() = PrimitiveKey<String>(stringPK(LAST_ADDED_CUTOFF_DURATION)) { Duration.Week(3).serialise() }

    val lastAddedCutoffTimeStamp
        get() = CompositeKey<Long>(LastAddedCutOffDurationPreferenceProvider)

    // Upgrade
    val checkUpgradeAtStartup
        get() = PrimitiveKey<Boolean>(booleanPK(CHECK_UPGRADE_AT_STARTUP)) { false }

    val _checkUpdateInterval
        get() = PrimitiveKey<String>(stringPK(CHECK_UPGRADE_INTERVAL)) { Duration.Day(1).serialise() }

    val checkUpdateInterval
        get() = CompositeKey<Duration>(CheckUpdateIntervalPreferenceProvider)

    val lastCheckUpgradeTimeStamp
        get() = PrimitiveKey<Long>(longPK(LAST_CHECK_UPGRADE_TIME)) { 0 }

    // List-SortMode
    val _songSortMode
        get() = PrimitiveKey<String>(stringPK(SONG_SORT_MODE)) { SortMode(SortRef.ID, false).serialize() }

    val songSortMode
        get() = CompositeKey<SortMode>(SortModePreferenceProvider.SongSortMode)


    val _albumSortMode
        get() = PrimitiveKey<String>(stringPK(ALBUM_SORT_MODE)) { SortMode(SortRef.ID, false).serialize() }

    val albumSortMode
        get() = CompositeKey<SortMode>(SortModePreferenceProvider.AlbumSortMode)


    val _artistSortMode
        get() = PrimitiveKey<String>(stringPK(ARTIST_SORT_MODE)) { SortMode(SortRef.ID, false).serialize() }

    val artistSortMode
        get() = CompositeKey<SortMode>(SortModePreferenceProvider.ArtistSortMode)


    val _genreSortMode
        get() = PrimitiveKey<String>(stringPK(GENRE_SORT_MODE)) { SortMode(SortRef.ID, false).serialize() }

    val genreSortMode
        get() = CompositeKey<SortMode>(SortModePreferenceProvider.GenreSortMode)


    val _fileSortMode
        get() = PrimitiveKey<String>(stringPK(FILE_SORT_MODE)) { SortMode(SortRef.ID, false).serialize() }

    val fileSortMode
        get() = CompositeKey<SortMode>(SortModePreferenceProvider.FileSortMode)


    val _collectionSortMode
        get() = PrimitiveKey<String>(stringPK(SONG_COLLECTION_SORT_MODE)) { SortMode(SortRef.ID, false).serialize() }

    val collectionSortMode
        get() = CompositeKey<SortMode>(SortModePreferenceProvider.CollectionSortMode)


    val _playlistSortMode
        get() = PrimitiveKey<String>(stringPK(PLAYLIST_SORT_MODE)) { SortMode(SortRef.ID, false).serialize() }

    val playlistSortMode
        get() = CompositeKey<SortMode>(SortModePreferenceProvider.PlaylistSortMode)


    // List-Appearance

    val albumArtistColoredFooters
        get() = PrimitiveKey<Boolean>(booleanPK(ALBUM_ARTIST_COLORED_FOOTERS)) { true }

    val albumColoredFooters
        get() = PrimitiveKey<Boolean>(booleanPK(ALBUM_COLORED_FOOTERS)) { true }

    val songColoredFooters
        get() = PrimitiveKey<Boolean>(booleanPK(SONG_COLORED_FOOTERS)) { true }

    val artistColoredFooters
        get() = PrimitiveKey<Boolean>(booleanPK(ARTIST_COLORED_FOOTERS)) { true }

    val showFileImages
        get() = PrimitiveKey<Boolean>(booleanPK(SHOW_FILE_IMAGINES)) { false }

    // ListPage-Appearance

    val _songItemLayout
        get() = PrimitiveKey<Int>(intPK(SONG_ITEM_LAYOUT)) { ItemLayoutStyle.LIST_EXTENDED.ordinal }

    val songItemLayout
        get() = CompositeKey<ItemLayoutStyle>(ItemLayoutProvider.SongItemLayoutProvider)

    val _songItemLayoutLand
        get() = PrimitiveKey<Int>(intPK(SONG_ITEM_LAYOUT_LAND)) { ItemLayoutStyle.LIST.ordinal }

    val songItemLayoutLand
        get() = CompositeKey<ItemLayoutStyle>(ItemLayoutProvider.LandSongItemLayoutProvider)

    val _albumItemLayout
        get() = PrimitiveKey<Int>(intPK(ALBUM_ITEM_LAYOUT)) { ItemLayoutStyle.LIST_3L.ordinal }

    val albumItemLayout
        get() = CompositeKey<ItemLayoutStyle>(ItemLayoutProvider.AlbumItemLayoutProvider)

    val _albumItemLayoutLand
        get() = PrimitiveKey<Int>(intPK(ALBUM_ITEM_LAYOUT_LAND)) { ItemLayoutStyle.LIST_3L.ordinal }

    val albumItemLayoutLand
        get() = CompositeKey<ItemLayoutStyle>(ItemLayoutProvider.LandAlbumItemLayoutProvider)

    val _artistItemLayout
        get() = PrimitiveKey<Int>(intPK(ARTIST_ITEM_LAYOUT)) { ItemLayoutStyle.LIST.ordinal }

    val artistItemLayout
        get() = CompositeKey<ItemLayoutStyle>(ItemLayoutProvider.ArtistItemLayoutProvider)

    val _artistItemLayoutLand
        get() = PrimitiveKey<Int>(intPK(ARTIST_ITEM_LAYOUT_LAND)) { ItemLayoutStyle.LIST_3L.ordinal }

    val artistItemLayoutLand
        get() = CompositeKey<ItemLayoutStyle>(ItemLayoutProvider.LandArtistItemLayoutProvider)

    val _folderItemLayout
        get() = PrimitiveKey<Int>(intPK(FOLDER_ITEM_LAYOUT)) { ItemLayoutStyle.LIST.ordinal }

    val folderItemLayout
        get() = CompositeKey<ItemLayoutStyle>(ItemLayoutProvider.FolderItemLayoutProvider)

    val _folderItemLayoutLand
        get() = PrimitiveKey<Int>(intPK(FOLDER_ITEM_LAYOUT_LAND)) { ItemLayoutStyle.LIST_3L.ordinal }

    val folderItemLayoutLand
        get() = CompositeKey<ItemLayoutStyle>(ItemLayoutProvider.LandFolderItemLayoutProvider)

    val songGridSize
        get() = PrimitiveKey<Int>(intPK(SONG_GRID_SIZE)) { 1 }

    val songGridSizeLand
        get() = PrimitiveKey<Int>(intPK(SONG_GRID_SIZE_LAND)) { 2 }

    val albumGridSize
        get() = PrimitiveKey<Int>(intPK(ALBUM_GRID_SIZE)) { 2 }

    val albumGridSizeLand
        get() = PrimitiveKey<Int>(intPK(ALBUM_GRID_SIZE_LAND)) { 3 }

    val artistGridSize
        get() = PrimitiveKey<Int>(intPK(ARTIST_GRID_SIZE)) { 3 }

    val artistGridSizeLand
        get() = PrimitiveKey<Int>(intPK(ARTIST_GRID_SIZE_LAND)) { 4 }

    val genreGridSize
        get() = PrimitiveKey<Int>(intPK(GENRE_GRID_SIZE)) { 1 }

    val genreGridSizeLand
        get() = PrimitiveKey<Int>(intPK(GENRE_GRID_SIZE_LAND)) { 2 }

    val playlistGridSize
        get() = PrimitiveKey<Int>(intPK(PLAYLIST_GRID_SIZE)) { 1 }

    val playlistGridSizeLand
        get() = PrimitiveKey<Int>(intPK(PLAYLIST_GRID_SIZE_LAND)) { 2 }

    val folderGridSize
        get() = PrimitiveKey<Int>(intPK(FOLDER_GRID_SIZE)) { 1 }

    val folderGridSizeLand
        get() = PrimitiveKey<Int>(intPK(FOLDER_GRID_SIZE_LAND)) { 2 }

    // SleepTimer
    val lastSleepTimerValue
        get() = PrimitiveKey<Int>(intPK(LAST_SLEEP_TIMER_VALUE)) { 30 }

    val nextSleepTimerElapsedRealTime
        get() = PrimitiveKey<Long>(longPK(NEXT_SLEEP_TIMER_ELAPSED_REALTIME)) { -1L }

    val sleepTimerFinishMusic
        get() = PrimitiveKey<Boolean>(booleanPK(SLEEP_TIMER_FINISH_SONG)) { false }

    // Misc
    val ignoreUpgradeDate
        get() = PrimitiveKey<Long>(longPK(IGNORE_UPGRADE_DATE)) { 0 }

    val pathFilterExcludeMode
        get() = PrimitiveKey<Boolean>(booleanPK(PATH_FILTER_EXCLUDE_MODE)) { true }

    // Compatibility
    val alwaysUseMediaSessionToDisplayCover
        get() = PrimitiveKey<Boolean>(booleanPK(ALWAYS_USE_MEDIA_SESSION_TO_DISPLAY_COVER)) { false }

    val useLegacyFavoritePlaylistImpl
        get() = PrimitiveKey<Boolean>(booleanPK(USE_LEGACY_FAVORITE_PLAYLIST_IMPL)) { false }

    val useLegacyListFilesImpl
        get() = PrimitiveKey<Boolean>(booleanPK(USE_LEGACY_LIST_FILES_IMPL)) { false }

    val disableRealTimeSearch
        get() = PrimitiveKey<Boolean>(booleanPK(DISABLE_REAL_TIME_SEARCH)) { false }

}