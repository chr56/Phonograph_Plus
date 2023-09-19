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

    operator fun <T> get(key: Keys<T>): Preference<T> = Preference(key,context)

    class Preference<T>(private val key: Keys<T>, context: Context) {

        private val dataStore = context.dataStore

        val flow: Flow<T>
            get() = dataStore.data.map { it[key.preferenceKey] ?: key.defaultValue }

        suspend fun flowData(): T = dataStore.data.first()[key.preferenceKey] ?: key.defaultValue

        suspend fun edit(value: () -> T) {
            dataStore.edit { mutablePreferences ->
                mutablePreferences[key.preferenceKey] = value()
            }
        }

        val data: T get() = runBlocking { flowData() }

        fun put(value: T) = runBlocking { edit { value } }

    }

}


@Suppress("ClassName")
sealed class Keys<T>(
    val preferenceKey: Preferences.Key<T>,
    val defaultValue: T,
) {
    data object homeTabConfigJsonString :
            Keys<String>(spk(HOME_TAB_CONFIG), "")

    data object coloredNotification :
            Keys<Boolean>(bpk(COLORED_NOTIFICATION), true)

    data object classicNotification :
            Keys<Boolean>(bpk(CLASSIC_NOTIFICATION), false)

    data object coloredAppShortcuts :
            Keys<Boolean>(bpk(COLORED_APP_SHORTCUTS), true)

    data object fixedTabLayout :
            Keys<Boolean>(bpk(FIXED_TAB_LAYOUT), false)

    // Behavior-Retention
    data object rememberLastTab :
            Keys<Boolean>(bpk(REMEMBER_LAST_TAB), true)

    data object lastPage :
            Keys<Int>(ipk(LAST_PAGE), 0)

    data object lastMusicChooser :
            Keys<Int>(ipk(LAST_MUSIC_CHOOSER), 0)

    data object nowPlayingScreenIndex :
            Keys<Int>(ipk(NOW_PLAYING_SCREEN_ID), 0)

    // Behavior-File
    data object imageSourceConfigJsonString :
            Keys<String>(spk(IMAGE_SOURCE_CONFIG), "{}")

    // Behavior-Playing
    data object songItemClickMode :
            Keys<Int>(ipk(SONG_ITEM_CLICK_MODE), SONG_PLAY_NOW)

    data object songItemClickExtraFlag :
            Keys<Int>(ipk(SONG_ITEM_CLICK_EXTRA_FLAG), FLAG_MASK_PLAY_QUEUE_IF_EMPTY)

    data object keepPlayingQueueIntact :
            Keys<Boolean>(bpk(KEEP_PLAYING_QUEUE_INTACT), true)

    data object rememberShuffle :
            Keys<Boolean>(bpk(REMEMBER_SHUFFLE), true)

    data object gaplessPlayback :
            Keys<Boolean>(bpk(GAPLESS_PLAYBACK), false)

    data object audioDucking :
            Keys<Boolean>(bpk(AUDIO_DUCKING), true)

    data object resumeAfterAudioFocusGain :
            Keys<Boolean>(bpk(RESUME_AFTER_AUDIO_FOCUS_GAIN), false)

    data object enableLyrics :
            Keys<Boolean>(bpk(ENABLE_LYRICS), true)

    data object broadcastSynchronizedLyrics :
            Keys<Boolean>(bpk(BROADCAST_SYNCHRONIZED_LYRICS), true)

    data object useLegacyStatusBarLyricsApi :
            Keys<Boolean>(bpk(USE_LEGACY_STATUS_BAR_LYRICS_API), false)

    data object broadcastCurrentPlayerState :
            Keys<Boolean>(bpk(BROADCAST_CURRENT_PLAYER_STATE), true)

    // Behavior-Lyrics
    data object synchronizedLyricsShow :
            Keys<Boolean>(bpk(SYNCHRONIZED_LYRICS_SHOW), true)

    data object displaySynchronizedLyricsTimeAxis :
            Keys<Boolean>(bpk(DISPLAY_LYRICS_TIME_AXIS), true)

    data object _lastAddedCutOffMode :
            Keys<Int>(ipk(LAST_ADDED_CUTOFF_MODE), TimeIntervalCalculationMode.PAST.value)

    data object _lastAddedCutOffDuration :
            Keys<String>(spk(LAST_ADDED_CUTOFF_DURATION), Duration.Week(3).serialise())

    // Upgrade
    data object checkUpgradeAtStartup :
            Keys<Boolean>(bpk(CHECK_UPGRADE_AT_STARTUP), false)

    data object _checkUpdateInterval :
            Keys<String>(spk(CHECK_UPGRADE_INTERVAL), Duration.Day(1).serialise())

    data object lastCheckUpgradeTimeStamp :
            Keys<Long>(lpk(LAST_CHECK_UPGRADE_TIME), 0)

    // List-SortMode
    data object _songSortMode :
            Keys<String>(spk(SONG_SORT_MODE), SortMode(SortRef.ID, false).serialize())

    data object _albumSortMode :
            Keys<String>(spk(ALBUM_SORT_MODE), SortMode(SortRef.ID, false).serialize())

    data object _artistSortMode :
            Keys<String>(spk(ARTIST_SORT_MODE), SortMode(SortRef.ID, false).serialize())

    data object _genreSortMode :
            Keys<String>(spk(GENRE_SORT_MODE), SortMode(SortRef.ID, false).serialize())

    data object _fileSortMode :
            Keys<String>(spk(FILE_SORT_MODE), FileSortMode(SortRef.ID, false).serialize())

    data object _collectionSortMode :
            Keys<String>(spk(SONG_COLLECTION_SORT_MODE), SortMode(SortRef.ID, false).serialize())

    data object _playlistSortMode :
            Keys<String>(spk(PLAYLIST_SORT_MODE), SortMode(SortRef.ID, false).serialize())


    // List-Appearance
    /*  see also [DisplaySetting] */
    data object albumArtistColoredFooters :
            Keys<Boolean>(bpk(ALBUM_ARTIST_COLORED_FOOTERS), true)

    data object showFileImages :
            Keys<Boolean>(bpk(SHOW_FILE_IMAGINES), false)

    // SleepTimer
    data object lastSleepTimerValue :
            Keys<Int>(ipk(LAST_SLEEP_TIMER_VALUE), 30)

    data object nextSleepTimerElapsedRealTime :
            Keys<Long>(lpk(NEXT_SLEEP_TIMER_ELAPSED_REALTIME), -1L)

    data object sleepTimerFinishMusic :
            Keys<Boolean>(bpk(SLEEP_TIMER_FINISH_SONG), false)

    // Misc
    data object ignoreUpgradeDate :
            Keys<Long>(lpk(IGNORE_UPGRADE_DATE), 0)

    data object pathFilterExcludeMode :
            Keys<Boolean>(bpk(PATH_FILTER_EXCLUDE_MODE), true)

    // Compatibility
    data object useLegacyFavoritePlaylistImpl :
            Keys<Boolean>(bpk(USE_LEGACY_FAVORITE_PLAYLIST_IMPL), false)

    data object useLegacyListFilesImpl :
            Keys<Boolean>(bpk(USE_LEGACY_LIST_FILES_IMPL), false)

    data object playlistFilesOperationBehaviour :
            Keys<String>(spk(PLAYLIST_FILES_OPERATION_BEHAVIOUR), PLAYLIST_OPS_BEHAVIOUR_AUTO)

    data object useLegacyDetailDialog :
            Keys<Boolean>(bpk(USE_LEGACY_DETAIL_DIALOG), false)

}
