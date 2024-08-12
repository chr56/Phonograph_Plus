/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.settings

import lib.phonograph.misc.MonetColor
import player.phonograph.model.SongClickMode
import player.phonograph.model.sort.SortMode
import player.phonograph.model.sort.SortRef
import player.phonograph.model.time.Duration
import player.phonograph.model.time.TimeIntervalCalculationMode
import player.phonograph.model.ItemLayoutStyle
import player.phonograph.model.NowPlayingScreen
import util.theme.materials.MaterialColor
import androidx.datastore.preferences.core.booleanPreferencesKey as booleanPK
import androidx.datastore.preferences.core.intPreferencesKey as intPK
import androidx.datastore.preferences.core.longPreferencesKey as longPK
import androidx.datastore.preferences.core.stringPreferencesKey as stringPK

/**
 * Container Object for all available registered [PreferenceKey]
 */
@Suppress("ClassName", "ConvertObjectToDataObject")
object Keys {

    // Appearance
    object theme :
            PrimitiveKey<String>(stringPK(THEME), { THEME_AUTO_LIGHTBLACK })

    object homeTabConfigJsonString :
            PrimitiveKey<String>(stringPK(HOME_TAB_CONFIG), { "" })

    object coloredAppShortcuts :
            PrimitiveKey<Boolean>(booleanPK(COLORED_APP_SHORTCUTS), { true })

    object fixedTabLayout :
            PrimitiveKey<Boolean>(booleanPK(FIXED_TAB_LAYOUT), { false })

    object coloredStatusbar :
            PrimitiveKey<Boolean>(booleanPK(COLORED_STATUSBAR), { true })

    object coloredNavigationBar :
            PrimitiveKey<Boolean>(booleanPK(COLORED_NAVIGATION_BAR), { false })

    object selectedPrimaryColor :
            PrimitiveKey<Int>(intPK(SELECTED_PRIMARY_COLOR), { MaterialColor.Blue._A400.asColor })

    object selectedAccentColor :
            PrimitiveKey<Int>(intPK(SELECTED_ACCENT_COLOR), { MaterialColor.Yellow._900.asColor })

    object enableMonet :
            PrimitiveKey<Boolean>(booleanPK(ENABLE_MONET), { false })

    object monetPalettePrimaryColor :
            PrimitiveKey<Int>(intPK(MONET_PALETTE_PRIMARY_COLOR), { MonetColor.defaultMonetPrimaryColor.value })

    object monetPaletteAccentColor :
            PrimitiveKey<Int>(intPK(MONET_PALETTE_ACCENT_COLOR), { MonetColor.defaultMonetAccentColor.value })


    // Appearance - Notification
    object coloredNotification :
            PrimitiveKey<Boolean>(booleanPK(COLORED_NOTIFICATION), { true })

    object classicNotification :
            PrimitiveKey<Boolean>(booleanPK(CLASSIC_NOTIFICATION), { false })

    object notificationActionsJsonString :
            PrimitiveKey<String>(stringPK(NOTIFICATION_ACTIONS), { "{}" })

    // Behavior-Retention
    object rememberLastTab :
            PrimitiveKey<Boolean>(booleanPK(REMEMBER_LAST_TAB), { true })

    object lastPage :
            PrimitiveKey<Int>(intPK(LAST_PAGE), { 0 })

    object nowPlayingScreenIndex :
            PrimitiveKey<Int>(intPK(NOW_PLAYING_SCREEN_ID), { 0 })

    object nowPlayingScreen :
            CompositeKey<NowPlayingScreen>(NowPlayingScreenPreferenceProvider)

    // Database

    // Behavior-File
    object preloadImages :
            PrimitiveKey<Boolean>(booleanPK(PRELOAD_IMAGES), { true })

    object imageSourceConfigJsonString :
            PrimitiveKey<String>(stringPK(IMAGE_SOURCE_CONFIG), { "{}" })

    object imageCache :
            PrimitiveKey<Boolean>(booleanPK(IMAGE_CACHE), { false })

    // Behavior-Playing
    object songItemClickMode :
            PrimitiveKey<Int>(intPK(SONG_ITEM_CLICK_MODE), { SongClickMode.SONG_PLAY_NOW })

    object songItemClickExtraFlag :
            PrimitiveKey<Int>(intPK(SONG_ITEM_CLICK_EXTRA_FLAG), { SongClickMode.FLAG_MASK_PLAY_QUEUE_IF_EMPTY })

    object externalPlayRequestShowPrompt :
            PrimitiveKey<Boolean>(booleanPK(EXTERNAL_PLAY_REQUEST_SHOW_PROMPT), { false })

    object externalPlayRequestSingleMode :
            PrimitiveKey<Int>(intPK(EXTERNAL_PLAY_REQUEST_SINGLE_MODE), { SongClickMode.SONG_PLAY_NOW })

    object externalPlayRequestMultipleMode :
            PrimitiveKey<Int>(intPK(EXTERNAL_PLAY_REQUEST_MULTIPLE_MODE), { SongClickMode.QUEUE_PLAY_NOW })

    object externalPlayRequestSilence :
            PrimitiveKey<Boolean>(booleanPK(EXTERNAL_PLAY_REQUEST_SILENCE), { false })

    object gaplessPlayback :
            PrimitiveKey<Boolean>(booleanPK(GAPLESS_PLAYBACK), { false })

    object audioDucking :
            PrimitiveKey<Boolean>(booleanPK(AUDIO_DUCKING), { true })

    object resumeAfterAudioFocusGain :
            PrimitiveKey<Boolean>(booleanPK(RESUME_AFTER_AUDIO_FOCUS_GAIN), { false })

    object alwaysPlay :
            PrimitiveKey<Boolean>(booleanPK(ALWAYS_PLAY), { false })

    object enableLyrics :
            PrimitiveKey<Boolean>(booleanPK(ENABLE_LYRICS), { true })

    object broadcastSynchronizedLyrics :
            PrimitiveKey<Boolean>(booleanPK(BROADCAST_SYNCHRONIZED_LYRICS), { true })

    object useLegacyStatusBarLyricsApi :
            PrimitiveKey<Boolean>(booleanPK(USE_LEGACY_STATUS_BAR_LYRICS_API), { false })

    object broadcastCurrentPlayerState :
            PrimitiveKey<Boolean>(booleanPK(BROADCAST_CURRENT_PLAYER_STATE), { true })

    object persistentPlaybackNotification :
            PrimitiveKey<Boolean>(booleanPK(PERSISTENT_PLAYBACK_NOTIFICATION), { false })

    // Behavior-Lyrics
    object synchronizedLyricsShow :
            PrimitiveKey<Boolean>(booleanPK(SYNCHRONIZED_LYRICS_SHOW), { true })

    object displaySynchronizedLyricsTimeAxis :
            PrimitiveKey<Boolean>(booleanPK(DISPLAY_LYRICS_TIME_AXIS), { true })

    object _lastAddedCutOffMode :
            PrimitiveKey<Int>(intPK(LAST_ADDED_CUTOFF_MODE), { TimeIntervalCalculationMode.PAST.value })

    object _lastAddedCutOffDuration :
            PrimitiveKey<String>(stringPK(LAST_ADDED_CUTOFF_DURATION), { Duration.Week(3).serialise() })

    object lastAddedCutoffTimeStamp :
            CompositeKey<Long>(LastAddedCutOffDurationPreferenceProvider)

    // Upgrade
    object checkUpgradeAtStartup :
            PrimitiveKey<Boolean>(booleanPK(CHECK_UPGRADE_AT_STARTUP), { false })

    object _checkUpdateInterval :
            PrimitiveKey<String>(stringPK(CHECK_UPGRADE_INTERVAL), { Duration.Day(1).serialise() })

    object checkUpdateInterval :
            CompositeKey<Duration>(CheckUpdateIntervalPreferenceProvider)

    object lastCheckUpgradeTimeStamp :
            PrimitiveKey<Long>(longPK(LAST_CHECK_UPGRADE_TIME), { 0 })

    // List-SortMode
    object _songSortMode :
            PrimitiveKey<String>(stringPK(SONG_SORT_MODE), { SortMode(SortRef.ID, false).serialize() })

    object songSortMode :
            CompositeKey<SortMode>(SortModePreferenceProvider.SongSortMode)


    object _albumSortMode :
            PrimitiveKey<String>(stringPK(ALBUM_SORT_MODE), { SortMode(SortRef.ID, false).serialize() })

    object albumSortMode :
            CompositeKey<SortMode>(SortModePreferenceProvider.AlbumSortMode)


    object _artistSortMode :
            PrimitiveKey<String>(stringPK(ARTIST_SORT_MODE), { SortMode(SortRef.ID, false).serialize() })

    object artistSortMode :
            CompositeKey<SortMode>(SortModePreferenceProvider.ArtistSortMode)


    object _genreSortMode :
            PrimitiveKey<String>(stringPK(GENRE_SORT_MODE), { SortMode(SortRef.ID, false).serialize() })

    object genreSortMode :
            CompositeKey<SortMode>(SortModePreferenceProvider.GenreSortMode)


    object _fileSortMode :
            PrimitiveKey<String>(stringPK(FILE_SORT_MODE), { SortMode(SortRef.ID, false).serialize() })

    object fileSortMode :
            CompositeKey<SortMode>(SortModePreferenceProvider.FileSortMode)


    object _collectionSortMode :
            PrimitiveKey<String>(stringPK(SONG_COLLECTION_SORT_MODE), { SortMode(SortRef.ID, false).serialize() })

    object collectionSortMode :
            CompositeKey<SortMode>(SortModePreferenceProvider.CollectionSortMode)


    object _playlistSortMode :
            PrimitiveKey<String>(stringPK(PLAYLIST_SORT_MODE), { SortMode(SortRef.ID, false).serialize() })

    object playlistSortMode :
            CompositeKey<SortMode>(SortModePreferenceProvider.PlaylistSortMode)


    // List-Appearance

    object albumArtistColoredFooters :
            PrimitiveKey<Boolean>(booleanPK(ALBUM_ARTIST_COLORED_FOOTERS), { true })

    object albumColoredFooters :
            PrimitiveKey<Boolean>(booleanPK(ALBUM_COLORED_FOOTERS), { true })

    object songColoredFooters :
            PrimitiveKey<Boolean>(booleanPK(SONG_COLORED_FOOTERS), { true })

    object artistColoredFooters :
            PrimitiveKey<Boolean>(booleanPK(ARTIST_COLORED_FOOTERS), { true })

    object showFileImages :
            PrimitiveKey<Boolean>(booleanPK(SHOW_FILE_IMAGINES), { false })

    // ListPage-Appearance

    object _songItemLayout :
            PrimitiveKey<Int>(intPK(SONG_ITEM_LAYOUT), { ItemLayoutStyle.LIST_EXTENDED.ordinal })

    object songItemLayout :
            CompositeKey<ItemLayoutStyle>(ItemLayoutProvider.SongItemLayoutProvider)

    object _songItemLayoutLand :
            PrimitiveKey<Int>(intPK(SONG_ITEM_LAYOUT_LAND), { ItemLayoutStyle.LIST.ordinal })

    object songItemLayoutLand :
            CompositeKey<ItemLayoutStyle>(ItemLayoutProvider.LandSongItemLayoutProvider)

    object _albumItemLayout :
            PrimitiveKey<Int>(intPK(ALBUM_ITEM_LAYOUT), { ItemLayoutStyle.LIST_3L.ordinal })

    object albumItemLayout :
            CompositeKey<ItemLayoutStyle>(ItemLayoutProvider.AlbumItemLayoutProvider)

    object _albumItemLayoutLand :
            PrimitiveKey<Int>(intPK(ALBUM_ITEM_LAYOUT_LAND), { ItemLayoutStyle.LIST_3L.ordinal })

    object albumItemLayoutLand :
            CompositeKey<ItemLayoutStyle>(ItemLayoutProvider.LandAlbumItemLayoutProvider)

    object _artistItemLayout :
            PrimitiveKey<Int>(intPK(ARTIST_ITEM_LAYOUT), { ItemLayoutStyle.LIST.ordinal })

    object artistItemLayout :
            CompositeKey<ItemLayoutStyle>(ItemLayoutProvider.ArtistItemLayoutProvider)

    object _artistItemLayoutLand :
            PrimitiveKey<Int>(intPK(ARTIST_ITEM_LAYOUT_LAND), { ItemLayoutStyle.LIST_3L.ordinal })

    object artistItemLayoutLand :
            CompositeKey<ItemLayoutStyle>(ItemLayoutProvider.LandArtistItemLayoutProvider)

    object songGridSize :
            PrimitiveKey<Int>(intPK(SONG_GRID_SIZE), { 1 })

    object songGridSizeLand :
            PrimitiveKey<Int>(intPK(SONG_GRID_SIZE_LAND), { 2 })

    object albumGridSize :
            PrimitiveKey<Int>(intPK(ALBUM_GRID_SIZE), { 2 })

    object albumGridSizeLand :
            PrimitiveKey<Int>(intPK(ALBUM_GRID_SIZE_LAND), { 3 })

    object artistGridSize :
            PrimitiveKey<Int>(intPK(ARTIST_GRID_SIZE), { 3 })

    object artistGridSizeLand :
            PrimitiveKey<Int>(intPK(ARTIST_GRID_SIZE_LAND), { 4 })

    object genreGridSize :
            PrimitiveKey<Int>(intPK(GENRE_GRID_SIZE), { 1 })

    object genreGridSizeLand :
            PrimitiveKey<Int>(intPK(GENRE_GRID_SIZE_LAND), { 2 })

    object playlistGridSize :
            PrimitiveKey<Int>(intPK(PLAYLIST_GRID_SIZE), { 1 })

    object playlistGridSizeLand :
            PrimitiveKey<Int>(intPK(PLAYLIST_GRID_SIZE_LAND), { 2 })

    // SleepTimer
    object lastSleepTimerValue :
            PrimitiveKey<Int>(intPK(LAST_SLEEP_TIMER_VALUE), { 30 })

    object nextSleepTimerElapsedRealTime :
            PrimitiveKey<Long>(longPK(NEXT_SLEEP_TIMER_ELAPSED_REALTIME), { -1L })

    object sleepTimerFinishMusic :
            PrimitiveKey<Boolean>(booleanPK(SLEEP_TIMER_FINISH_SONG), { false })

    // Misc
    object ignoreUpgradeDate :
            PrimitiveKey<Long>(longPK(IGNORE_UPGRADE_DATE), { 0 })

    object pathFilterExcludeMode :
            PrimitiveKey<Boolean>(booleanPK(PATH_FILTER_EXCLUDE_MODE), { true })

    // Compatibility
    object alwaysUseMediaSessionToDisplayCover :
            PrimitiveKey<Boolean>(booleanPK(ALWAYS_USE_MEDIA_SESSION_TO_DISPLAY_COVER), { false })

    object useLegacyFavoritePlaylistImpl :
            PrimitiveKey<Boolean>(booleanPK(USE_LEGACY_FAVORITE_PLAYLIST_IMPL), { false })

    object useLegacyListFilesImpl :
            PrimitiveKey<Boolean>(booleanPK(USE_LEGACY_LIST_FILES_IMPL), { false })

    object playlistFilesOperationBehaviour :
            PrimitiveKey<String>(stringPK(PLAYLIST_FILES_OPERATION_BEHAVIOUR), { PLAYLIST_OPS_BEHAVIOUR_AUTO })

    object disableRealTimeSearch :
            PrimitiveKey<Boolean>(booleanPK(DISABLE_REAL_TIME_SEARCH), { false })

}