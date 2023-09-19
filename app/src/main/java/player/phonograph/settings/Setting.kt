/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.settings

import player.phonograph.actions.click.mode.SongClickMode.FLAG_MASK_PLAY_QUEUE_IF_EMPTY
import player.phonograph.actions.click.mode.SongClickMode.SONG_PLAY_NOW
import player.phonograph.model.sort.FileSortMode
import player.phonograph.model.sort.SortMode
import player.phonograph.model.sort.SortRef
import player.phonograph.model.time.Duration
import player.phonograph.model.time.TimeIntervalCalculationMode
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import androidx.datastore.preferences.core.booleanPreferencesKey as bpk
import androidx.datastore.preferences.core.intPreferencesKey as ipk
import androidx.datastore.preferences.core.longPreferencesKey as lpk
import androidx.datastore.preferences.core.stringPreferencesKey as spk


class SettingStore(val context: Context) {

    operator fun <T> get(key: PrimitiveKey<T>): PrimitivePreference<T> =
        PrimitivePreference(key, context)

    @Suppress("PropertyName")
    val Composites = object {
        operator fun <T> get(key: CompositeKey<T>): CompositePreference<T> =
            CompositePreference(key, context)
    }

}


class PrimitivePreference<T>(private val key: PrimitiveKey<T>, context: Context) {

    private val dataStore = context.dataStore

    val flow: Flow<T>
        get() = dataStore.data.map { it[key.preferenceKey] ?: key.defaultValue() }

    suspend fun flowData(): T = dataStore.data.first()[key.preferenceKey] ?: key.defaultValue()

    suspend fun edit(value: () -> T) {
        dataStore.edit { mutablePreferences ->
            mutablePreferences[key.preferenceKey] = value()
        }
    }

    val data: T get() = runBlocking { flowData() }

    fun put(value: T) = runBlocking { edit { value } }

}


class CompositePreference<T>(private val key: CompositeKey<T>, context: Context) {

    private val dataStore = context.dataStore

}

sealed interface PreferenceKey<T>

/**
 * Key container of composite type preference
 */
sealed class CompositeKey<T>(
    val defaultValue: () -> T,
) : PreferenceKey<T>


/**
 * Key container of primitive type preference
 */
sealed class PrimitiveKey<T>(
    val preferenceKey: Preferences.Key<T>,
    val defaultValue: () -> T,
) : PreferenceKey<T> {}


@Suppress("ClassName")
object Keys {

    // Appearance
    data object homeTabConfigJsonString :
            PrimitiveKey<String>(spk(HOME_TAB_CONFIG), { "" })

    data object coloredNotification :
            PrimitiveKey<Boolean>(bpk(COLORED_NOTIFICATION), { true })

    data object classicNotification :
            PrimitiveKey<Boolean>(bpk(CLASSIC_NOTIFICATION), { false })

    data object coloredAppShortcuts :
            PrimitiveKey<Boolean>(bpk(COLORED_APP_SHORTCUTS), { true })

    data object fixedTabLayout :
            PrimitiveKey<Boolean>(bpk(FIXED_TAB_LAYOUT), { false })

    // Behavior-Retention
    data object rememberLastTab :
            PrimitiveKey<Boolean>(bpk(REMEMBER_LAST_TAB), { true })

    data object lastPage :
            PrimitiveKey<Int>(ipk(LAST_PAGE), { 0 })

    data object lastMusicChooser :
            PrimitiveKey<Int>(ipk(LAST_MUSIC_CHOOSER), { 0 })

    data object nowPlayingScreenIndex :
            PrimitiveKey<Int>(ipk(NOW_PLAYING_SCREEN_ID), { 0 })

    // Behavior-File
    data object imageSourceConfigJsonString :
            PrimitiveKey<String>(spk(IMAGE_SOURCE_CONFIG), { "{}" })

    // Behavior-Playing
    data object songItemClickMode :
            PrimitiveKey<Int>(ipk(SONG_ITEM_CLICK_MODE), { SONG_PLAY_NOW })

    data object songItemClickExtraFlag :
            PrimitiveKey<Int>(ipk(SONG_ITEM_CLICK_EXTRA_FLAG), { FLAG_MASK_PLAY_QUEUE_IF_EMPTY })

    data object keepPlayingQueueIntact :
            PrimitiveKey<Boolean>(bpk(KEEP_PLAYING_QUEUE_INTACT), { true })

    data object rememberShuffle :
            PrimitiveKey<Boolean>(bpk(REMEMBER_SHUFFLE), { true })

    data object gaplessPlayback :
            PrimitiveKey<Boolean>(bpk(GAPLESS_PLAYBACK), { false })

    data object audioDucking :
            PrimitiveKey<Boolean>(bpk(AUDIO_DUCKING), { true })

    data object resumeAfterAudioFocusGain :
            PrimitiveKey<Boolean>(bpk(RESUME_AFTER_AUDIO_FOCUS_GAIN), { false })

    data object enableLyrics :
            PrimitiveKey<Boolean>(bpk(ENABLE_LYRICS), { true })

    data object broadcastSynchronizedLyrics :
            PrimitiveKey<Boolean>(bpk(BROADCAST_SYNCHRONIZED_LYRICS), { true })

    data object useLegacyStatusBarLyricsApi :
            PrimitiveKey<Boolean>(bpk(USE_LEGACY_STATUS_BAR_LYRICS_API), { false })

    data object broadcastCurrentPlayerState :
            PrimitiveKey<Boolean>(bpk(BROADCAST_CURRENT_PLAYER_STATE), { true })

    // Behavior-Lyrics
    data object synchronizedLyricsShow :
            PrimitiveKey<Boolean>(bpk(SYNCHRONIZED_LYRICS_SHOW), { true })

    data object displaySynchronizedLyricsTimeAxis :
            PrimitiveKey<Boolean>(bpk(DISPLAY_LYRICS_TIME_AXIS), { true })

    data object _lastAddedCutOffMode :
            PrimitiveKey<Int>(ipk(LAST_ADDED_CUTOFF_MODE), { TimeIntervalCalculationMode.PAST.value })

    data object _lastAddedCutOffDuration :
            PrimitiveKey<String>(spk(LAST_ADDED_CUTOFF_DURATION), { Duration.Week(3).serialise() })

    // Upgrade
    data object checkUpgradeAtStartup :
            PrimitiveKey<Boolean>(bpk(CHECK_UPGRADE_AT_STARTUP), { false })

    data object _checkUpdateInterval :
            PrimitiveKey<String>(spk(CHECK_UPGRADE_INTERVAL), { Duration.Day(1).serialise() })

    data object lastCheckUpgradeTimeStamp :
            PrimitiveKey<Long>(lpk(LAST_CHECK_UPGRADE_TIME), { 0 })

    // List-SortMode
    data object _songSortMode :
            PrimitiveKey<String>(spk(SONG_SORT_MODE), { SortMode(SortRef.ID, false).serialize() })

    data object _albumSortMode :
            PrimitiveKey<String>(spk(ALBUM_SORT_MODE), { SortMode(SortRef.ID, false).serialize() })

    data object _artistSortMode :
            PrimitiveKey<String>(spk(ARTIST_SORT_MODE), { SortMode(SortRef.ID, false).serialize() })

    data object _genreSortMode :
            PrimitiveKey<String>(spk(GENRE_SORT_MODE), { SortMode(SortRef.ID, false).serialize() })

    data object _fileSortMode :
            PrimitiveKey<String>(spk(FILE_SORT_MODE), { FileSortMode(SortRef.ID, false).serialize() })

    data object _collectionSortMode :
            PrimitiveKey<String>(spk(SONG_COLLECTION_SORT_MODE), { SortMode(SortRef.ID, false).serialize() })

    data object _playlistSortMode :
            PrimitiveKey<String>(spk(PLAYLIST_SORT_MODE), { SortMode(SortRef.ID, false).serialize() })


    // List-Appearance

    data object albumArtistColoredFooters :
            PrimitiveKey<Boolean>(bpk(ALBUM_ARTIST_COLORED_FOOTERS), { true })

    data object showFileImages :
            PrimitiveKey<Boolean>(bpk(SHOW_FILE_IMAGINES), { false })

    // SleepTimer
    data object lastSleepTimerValue :
            PrimitiveKey<Int>(ipk(LAST_SLEEP_TIMER_VALUE), { 30 })

    data object nextSleepTimerElapsedRealTime :
            PrimitiveKey<Long>(lpk(NEXT_SLEEP_TIMER_ELAPSED_REALTIME), { -1L })

    data object sleepTimerFinishMusic :
            PrimitiveKey<Boolean>(bpk(SLEEP_TIMER_FINISH_SONG), { false })

    // Misc
    data object ignoreUpgradeDate :
            PrimitiveKey<Long>(lpk(IGNORE_UPGRADE_DATE), { 0 })

    data object pathFilterExcludeMode :
            PrimitiveKey<Boolean>(bpk(PATH_FILTER_EXCLUDE_MODE), { true })

    // Compatibility
    data object useLegacyFavoritePlaylistImpl :
            PrimitiveKey<Boolean>(bpk(USE_LEGACY_FAVORITE_PLAYLIST_IMPL), { false })

    data object useLegacyListFilesImpl :
            PrimitiveKey<Boolean>(bpk(USE_LEGACY_LIST_FILES_IMPL), { false })

    data object playlistFilesOperationBehaviour :
            PrimitiveKey<String>(spk(PLAYLIST_FILES_OPERATION_BEHAVIOUR), { PLAYLIST_OPS_BEHAVIOUR_AUTO })

    data object useLegacyDetailDialog :
            PrimitiveKey<Boolean>(bpk(USE_LEGACY_DETAIL_DIALOG), { false })

}
