/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.settings

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.preference.PreferenceManager
import player.phonograph.BaseApp
import player.phonograph.R
import player.phonograph.model.sort.FileSortMode
import player.phonograph.model.sort.SortMode
import player.phonograph.model.sort.SortRef
import player.phonograph.util.CalendarUtil
import player.phonograph.util.FileUtil.defaultStartDirectory
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class Setting(context: Context) {

    private val mPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(
        context
    )
    private val editor: SharedPreferences.Editor = mPreferences.edit()

    /**
     * @return main SharedPreferences
     */
    val rawMainPreference get() = mPreferences

    fun registerOnSharedPreferenceChangedListener(
        sharedPreferenceChangeListener: SharedPreferences.OnSharedPreferenceChangeListener?,
    ) {
        mPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener)
    }

    fun unregisterOnSharedPreferenceChangedListener(
        sharedPreferenceChangeListener: SharedPreferences.OnSharedPreferenceChangeListener?,
    ) {
        mPreferences.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener)
    }

    // Theme and Color

    var themeString: String by StringPref(GENERAL_THEME, "auto")

    // Appearance
    var homeTabConfigJsonString: String by StringPref(HOME_TAB_CONFIG, "")
    var coloredNotification: Boolean by BooleanPref(COLORED_NOTIFICATION, true)
    var classicNotification: Boolean by BooleanPref(CLASSIC_NOTIFICATION, false)
    var coloredAppShortcuts: Boolean by BooleanPref(COLORED_APP_SHORTCUTS, true)
    var albumArtOnLockscreen: Boolean by BooleanPref(ALBUM_ART_ON_LOCKSCREEN, true)
    var blurredAlbumArt: Boolean by BooleanPref(BLURRED_ALBUM_ART, false)
    var fixedTabLayout: Boolean by BooleanPref(FIXED_TAB_LAYOUT, false)

    // Behavior-Retention
    var rememberLastTab: Boolean by BooleanPref(REMEMBER_LAST_TAB, true)
    var lastPage: Int by IntPref(LAST_PAGE, 0)
    var lastMusicChooser: Int by IntPref(LAST_MUSIC_CHOOSER, 0)
    var nowPlayingScreenIndex: Int by IntPref(NOW_PLAYING_SCREEN_ID, 0)

    // Behavior-File
    // var ignoreMediaStoreArtwork: Boolean by BooleanPref(IGNORE_MEDIA_STORE_ARTWORK, false) // moved to `common`
    var autoDownloadImagesPolicy: String by StringPref(AUTO_DOWNLOAD_IMAGES_POLICY, "never")

    // Behavior-Playing
    var keepPlayingQueueIntact: Boolean by BooleanPref(KEEP_PLAYING_QUEUE_INTACT, true)
    var rememberShuffle: Boolean by BooleanPref(REMEMBER_SHUFFLE, true)
    var gaplessPlayback: Boolean by BooleanPref(GAPLESS_PLAYBACK, false)
    var audioDucking: Boolean by BooleanPref(AUDIO_DUCKING, true)
    var broadcastSynchronizedLyrics: Boolean by BooleanPref(BROADCAST_SYNCHRONIZED_LYRICS, true)
    var broadcastCurrentPlayerState: Boolean by BooleanPref(BROADCAST_CURRENT_PLAYER_STATE, true)

    // Behavior-Lyrics
    var synchronizedLyricsShow: Boolean by BooleanPref(SYNCHRONIZED_LYRICS_SHOW, true)
    var displaySynchronizedLyricsTimeAxis: Boolean by BooleanPref(DISPLAY_LYRICS_TIME_AXIS, true)

    // Upgrade
    var checkUpgradeAtStartup: Boolean by BooleanPref(CHECK_UPGRADE_AT_STARTUP, true)

    // List-SortMode
    private var _songSortMode: String by StringPref(
        SONG_SORT_MODE,
        SortMode(SortRef.ID, false).serialize()
    )
    var songSortMode: SortMode
        get() = SortMode.deserialize(_songSortMode)
        set(value) {
            _songSortMode = value.serialize()
        }

    private var _albumSortMode: String by StringPref(
        ALBUM_SORT_MODE,
        SortMode(SortRef.ID, false).serialize()
    )
    var albumSortMode: SortMode
        get() = SortMode.deserialize(_albumSortMode)
        set(value) {
            _albumSortMode = value.serialize()
        }

    private var _artistSortMode: String by StringPref(
        ARTIST_SORT_MODE,
        SortMode(SortRef.ID, false).serialize()
    )
    var artistSortMode: SortMode
        get() = SortMode.deserialize(_artistSortMode)
        set(value) {
            _artistSortMode = value.serialize()
        }

    private var _genreSortMode: String by StringPref(
        GENRE_SORT_MODE,
        SortMode(SortRef.ID, false).serialize()
    )
    var genreSortMode: SortMode
        get() = SortMode.deserialize(_genreSortMode)
        set(value) {
            _genreSortMode = value.serialize()
        }

    // List-Appearance
    var albumGridSize: Int by IntPref(
        ALBUM_GRID_SIZE,
        BaseApp.instance.resources.getInteger(R.integer.default_grid_columns)
    )
    var songGridSize: Int by IntPref(
        SONG_GRID_SIZE,
        BaseApp.instance.resources.getInteger(R.integer.default_list_columns)
    )
    var artistGridSize: Int by IntPref(
        ARTIST_GRID_SIZE,
        BaseApp.instance.resources.getInteger(R.integer.default_list_columns)
    )
    var genreGridSize: Int by IntPref(
        GENRE_GRID_SIZE,
        BaseApp.instance.resources.getInteger(R.integer.default_list_columns)
    )
    var albumGridSizeLand: Int by IntPref(
        ALBUM_GRID_SIZE_LAND,
        BaseApp.instance.resources.getInteger(R.integer.default_grid_columns_land)
    )
    var songGridSizeLand: Int by IntPref(
        SONG_GRID_SIZE_LAND,
        BaseApp.instance.resources.getInteger(R.integer.default_grid_columns_land)
    )
    var artistGridSizeLand: Int by IntPref(
        ARTIST_GRID_SIZE_LAND,
        BaseApp.instance.resources.getInteger(R.integer.default_grid_columns_land)
    )
    var genreGridSizeLand: Int by IntPref(
        GENRE_GRID_SIZE_LAND,
        BaseApp.instance.resources.getInteger(R.integer.default_grid_columns_land)
    )
    var albumColoredFooters by BooleanPref(ALBUM_COLORED_FOOTERS, true)
    var albumArtistColoredFooters by BooleanPref(ALBUM_ARTIST_COLORED_FOOTERS, true)
    var songColoredFooters by BooleanPref(SONG_COLORED_FOOTERS, true)
    var artistColoredFooters by BooleanPref(ARTIST_COLORED_FOOTERS, true)

    var showFileImages by BooleanPref(SHOW_FILE_IMAGINES, false)

    // SleepTimer
    var lastSleepTimerValue: Int by IntPref(LAST_SLEEP_TIMER_VALUE, 30)
    var nextSleepTimerElapsedRealTime: Long by LongPref(NEXT_SLEEP_TIMER_ELAPSED_REALTIME, -1L)
    var sleepTimerFinishMusic: Boolean by BooleanPref(SLEEP_TIMER_FINISH_SONG, false)

    // Misc
    var startDirectoryPath: String by StringPref(START_DIRECTORY, defaultStartDirectory.path)
    var ignoreUpgradeVersionCode: Int by IntPref(IGNORE_UPGRADE_VERSION_CODE, 0)

    // Compatibility
    var useLegacyFavoritePlaylistImpl: Boolean by BooleanPref(
        USE_LEGACY_FAVORITE_PLAYLIST_IMPL,
        false
    )
    var useLegacyListFilesImpl: Boolean by BooleanPref(USE_LEGACY_LIST_FILES_IMPL, false)
    var playlistFilesOperationBehaviour: String by StringPref(
        PLAYLIST_FILES_OPERATION_BEHAVIOUR,
        PLAYLIST_OPS_BEHAVIOUR_AUTO
    )

    // Changelog
    var lastChangeLogVersion: Int by IntPref(LAST_CHANGELOG_VERSION, -1)
    var introShown: Boolean by BooleanPref(INTRO_SHOWN, false)

    companion object {
        private const val TAG = "Setting"

        const val GENERAL_THEME = "general_theme"

        // Appearance
        const val HOME_TAB_CONFIG = "home_tab_config"
        const val COLORED_NOTIFICATION = "colored_notification"
        const val CLASSIC_NOTIFICATION = "classic_notification"
        private const val COLORED_APP_SHORTCUTS = "colored_app_shortcuts"
        const val ALBUM_ART_ON_LOCKSCREEN = "album_art_on_lockscreen"
        const val BLURRED_ALBUM_ART = "blurred_album_art"
        const val FIXED_TAB_LAYOUT = "fixed_tab_layout"

        // Behavior-Retention
        private const val REMEMBER_LAST_TAB = "remember_last_tab"
        private const val LAST_PAGE = "last_start_page"
        private const val LAST_MUSIC_CHOOSER = "last_music_chooser"
        const val NOW_PLAYING_SCREEN_ID = "now_playing_screen_id"

        // Behavior-File
        // const val IGNORE_MEDIA_STORE_ARTWORK = "ignore_media_store_artwork" // moved to `common`
        private const val AUTO_DOWNLOAD_IMAGES_POLICY = "auto_download_images_policy"

        // Behavior-Playing
        const val KEEP_PLAYING_QUEUE_INTACT = "keep_playing_queue_intact"
        private const val REMEMBER_SHUFFLE = "remember_shuffle"
        private const val AUDIO_DUCKING = "audio_ducking"
        const val GAPLESS_PLAYBACK = "gapless_playback"
        const val BROADCAST_SYNCHRONIZED_LYRICS = "synchronized_lyrics_send"
        const val BROADCAST_CURRENT_PLAYER_STATE = "broadcast_current_player_state"

        // Behavior-Lyrics
        private const val SYNCHRONIZED_LYRICS_SHOW = "synchronized_lyrics_show"
        private const val DISPLAY_LYRICS_TIME_AXIS = "display_lyrics_time_axis"

        // Upgrade
        private const val CHECK_UPGRADE_AT_STARTUP = "check_upgrade_at_startup"

        // List-SortMode
        private const val SONG_SORT_MODE = "song_sort_mode"
        private const val ALBUM_SORT_MODE = "album_sort_mode"
        private const val ARTIST_SORT_MODE = "artist_sort_mode"
        private const val GENRE_SORT_MODE = "genre_sort_mode"

        private const val FILE_SORT_MODE = "file_sort_mode"

        // List-Appearance
        private const val ALBUM_GRID_SIZE = "album_grid_size"
        private const val ALBUM_GRID_SIZE_LAND = "album_grid_size_land"
        private const val SONG_GRID_SIZE = "song_grid_size"
        private const val SONG_GRID_SIZE_LAND = "song_grid_size_land"
        private const val ARTIST_GRID_SIZE = "artist_grid_size"
        private const val ARTIST_GRID_SIZE_LAND = "artist_grid_size_land"
        private const val GENRE_GRID_SIZE = "genre_grid_size"
        private const val GENRE_GRID_SIZE_LAND = "genre_grid_size_land"
        private const val ALBUM_COLORED_FOOTERS = "album_colored_footers"
        private const val SONG_COLORED_FOOTERS = "song_colored_footers"
        private const val ARTIST_COLORED_FOOTERS = "artist_colored_footers"
        private const val ALBUM_ARTIST_COLORED_FOOTERS = "album_artist_colored_footers"
        private const val SHOW_FILE_IMAGINES = "show_file_imagines"

        // SleepTimer
        private const val LAST_SLEEP_TIMER_VALUE = "last_sleep_timer_value"
        private const val NEXT_SLEEP_TIMER_ELAPSED_REALTIME = "next_sleep_timer_elapsed_real_time"
        private const val SLEEP_TIMER_FINISH_SONG = "sleep_timer_finish_music"

        // Misc
        private const val START_DIRECTORY = "start_directory"
        private const val IGNORE_UPGRADE_VERSION_CODE = "ignore_upgrade_version_code"

        // compatibility
        private const val USE_LEGACY_FAVORITE_PLAYLIST_IMPL = "use_legacy_favorite_playlist_impl"
        private const val USE_LEGACY_LIST_FILES_IMPL = "use_legacy_list_files_impl"
        private const val PLAYLIST_FILES_OPERATION_BEHAVIOUR = "playlist_files_operation_behaviour"

        // Changelog
        private const val LAST_CHANGELOG_VERSION = "last_changelog_version"
        private const val INTRO_SHOWN = "intro_shown"

        // unused & deprecated
        const val FORCE_SQUARE_ALBUM_COVER = "force_square_album_art"

        //
        // Singleton
        //
        private var singleton: Setting? = null
        val instance: Setting
            get() {
                if (singleton == null) singleton = Setting(BaseApp.instance)
                return singleton!!
            }

        @JvmStatic
        fun instance(): Setting = instance

        @JvmStatic
        fun isAllowedToDownloadMetadata(context: Context): Boolean {
            return when (Setting.instance.autoDownloadImagesPolicy) {
                "always" -> true
                "only_wifi" -> {
                    val cm =
                        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

                    if (!cm.isActiveNetworkMetered) return false // we pass first metred Wifi and Cellular
                    val network = cm.activeNetwork ?: return false // no active network?
                    val capabilities =
                        cm.getNetworkCapabilities(network) ?: return false // no capabilities?
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                }
                "never" -> false
                else -> false
            }
        }

        const val PLAYLIST_OPS_BEHAVIOUR_AUTO = "auto"
        const val PLAYLIST_OPS_BEHAVIOUR_FORCE_SAF = "force_saf"
        const val PLAYLIST_OPS_BEHAVIOUR_FORCE_LEGACY = "force_legacy"

    }

    // Delegates

    inner class StringPref(private val keyName: String, private val defaultValue: String) : ReadWriteProperty<Any?, String> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): String =
            mPreferences.getString(keyName, defaultValue) ?: defaultValue

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
            editor.putString(keyName, value).apply()
        }
    }

    inner class BooleanPref(private val keyName: String, private val defaultValue: Boolean) : ReadWriteProperty<Any?, Boolean> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): Boolean =
            mPreferences.getBoolean(keyName, defaultValue)

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) {
            editor.putBoolean(keyName, value).apply()
        }
    }

    inner class IntPref(private val keyName: String, private val defaultValue: Int) : ReadWriteProperty<Any?, Int> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): Int =
            mPreferences.getInt(keyName, defaultValue)

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
            editor.putInt(keyName, value).apply()
        }
    }

    inner class LongPref(private val keyName: String, private val defaultValue: Long) : ReadWriteProperty<Any?, Long> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): Long =
            mPreferences.getLong(keyName, defaultValue)

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Long) {
            editor.putLong(keyName, value).apply()
        }
    }
}
