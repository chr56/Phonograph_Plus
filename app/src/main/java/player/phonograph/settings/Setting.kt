/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.settings

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import android.widget.Toast
import androidx.annotation.StyleRes
import androidx.preference.PreferenceManager
import java.io.File
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import org.json.JSONException
import org.json.JSONObject
import player.phonograph.App
import player.phonograph.R
import player.phonograph.adapter.PageConfig
import player.phonograph.adapter.PageConfigUtil
import player.phonograph.mediastore.sort.SortMode
import player.phonograph.mediastore.sort.SortRef
import player.phonograph.ui.fragments.mainactivity.folders.FoldersFragment
import player.phonograph.ui.fragments.player.NowPlayingScreen
import player.phonograph.util.CalendarUtil
import player.phonograph.util.FileUtil
import util.mdcolor.pref.ThemeColor

class Setting(context: Context) {

    private val mPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val editor: SharedPreferences.Editor = mPreferences.edit()

    fun registerOnSharedPreferenceChangedListener(
        sharedPreferenceChangeListener: SharedPreferences.OnSharedPreferenceChangeListener?
    ) {
        mPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener)
    }

    fun unregisterOnSharedPreferenceChangedListener(
        sharedPreferenceChangeListener: SharedPreferences.OnSharedPreferenceChangeListener?
    ) {
        mPreferences.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener)
    }

    /**
     * **WARNING**! to reset all SharedPreferences!
     */
    @SuppressLint("ApplySharedPref") // must do immediately!
    fun clearAllPreference() {
        editor.clear().commit()
        // lib
        ThemeColor.editTheme(App.instance).clearAllPreference()
        Toast.makeText(App.instance, R.string.success, Toast.LENGTH_SHORT).show()
    }

    // Theme and Color
    @get:StyleRes
    val generalTheme: Int
        get() = getThemeResFromPrefValue(
            mPreferences.getString(GENERAL_THEME, "auto")
        )
    fun setGeneralTheme(theme: String?) {
        val editor = mPreferences.edit()
        editor.putString(GENERAL_THEME, theme)
        editor.apply()
    }

    // Appearance
    var homeTabConfig: PageConfig
        get() {
            val rawString = mPreferences.getString(HOME_TAB_CONFIG, null)
            val config: PageConfig = try {
                PageConfigUtil.fromJson(JSONObject(rawString ?: ""))
            } catch (e: JSONException) {
                Log.e("Preference", "home tab config string $rawString")
                Log.e("Preference", "Fail to parse home tab config string\n ${e.message}")
                // return default
                PageConfig.DEFAULT_CONFIG
            }
            // valid // TODO
            return config
        }
        set(value) {
            val json =
                try {
                    PageConfigUtil.toJson(value)
                } catch (e: JSONException) {
                    Log.e("Preference", "Save home tab config failed, use default. \n${e.message}")
                    // return default
                    PageConfigUtil.DEFAULT_CONFIG
                }
            val editor = mPreferences.edit()
            editor.putString(HOME_TAB_CONFIG, json.toString(0))
            editor.apply()
        }

    fun resetHomeTabConfig() {
        editor.putString(HOME_TAB_CONFIG, PageConfigUtil.DEFAULT_CONFIG.toString(0)).apply()
    }

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
    var nowPlayingScreen: NowPlayingScreen
        get() {
            val id = mPreferences.getInt(NOW_PLAYING_SCREEN_ID, 0)
            for (nowPlayingScreen in NowPlayingScreen.values()) {
                if (nowPlayingScreen.id == id) return nowPlayingScreen
            }
            return NowPlayingScreen.CARD
        }
        set(nowPlayingScreen) {
            val editor = mPreferences.edit()
            editor.putInt(NOW_PLAYING_SCREEN_ID, nowPlayingScreen.id)
            editor.apply()
        }

    // Behavior-File
    var ignoreMediaStoreArtwork: Boolean by BooleanPref(IGNORE_MEDIA_STORE_ARTWORK, false)
    var autoDownloadImagesPolicy: String by StringPref(AUTO_DOWNLOAD_IMAGES_POLICY, "never")

    // Behavior-Playing
    var rememberShuffle: Boolean by BooleanPref(REMEMBER_SHUFFLE, true)
    var gaplessPlayback: Boolean by BooleanPref(GAPLESS_PLAYBACK, false)
    var audioDucking: Boolean by BooleanPref(AUDIO_DUCKING, true)

    // Behavior-Lyrics
    var synchronizedLyricsShow: Boolean by BooleanPref(SYNCHRONIZED_LYRICS_SHOW, true)
    var displaySynchronizedLyricsTimeAxis: Boolean by BooleanPref(DISPLAY_LYRICS_TIME_AXIS, true)
    var broadcastSynchronizedLyrics: Boolean by BooleanPref(BROADCAST_SYNCHRONIZED_LYRICS, true)

    // List-Cutoff
    val lastAddedCutoff: Long
        get() {
            val interval: Long = when (mPreferences.getString(LAST_ADDED_CUTOFF, "")) {
                "today" -> CalendarUtil.elapsedToday
                "this_week" -> CalendarUtil.elapsedWeek
                "past_seven_days" -> CalendarUtil.getElapsedDays(7)
                "past_three_months" -> CalendarUtil.getElapsedMonths(3)
                "this_year" -> CalendarUtil.elapsedYear
                "this_month" -> CalendarUtil.elapsedMonth
                else -> CalendarUtil.elapsedMonth
            }
            return (System.currentTimeMillis() - interval) / 1000
        }

    // Upgrade
    var checkUpgradeAtStartup: Boolean by BooleanPref(CHECK_UPGRADE_AT_STARTUP, true)

    // List-SortMode
    private var _songSortMode: String by StringPref(SONG_SORT_MODE, SortMode(SortRef.ID, false).serialize())
    var songSortMode: SortMode
        get() = SortMode.deserialize(_songSortMode)
        set(value) {
            _songSortMode = value.serialize()
        }

    private var _albumSortMode: String by StringPref(ALBUM_SORT_MODE, SortMode(SortRef.ID, false).serialize())
    var albumSortMode: SortMode
        get() = SortMode.deserialize(_albumSortMode)
        set(value) {
            _albumSortMode = value.serialize()
        }

    private var _artistSortMode: String by StringPref(ARTIST_SORT_MODE, SortMode(SortRef.ID, false).serialize())
    var artistSortMode: SortMode
        get() = SortMode.deserialize(_artistSortMode)
        set(value) {
            _artistSortMode = value.serialize()
        }

    private var _genreSortMode: String by StringPref(GENRE_SORT_MODE, SortMode(SortRef.ID, false).serialize())
    var genreSortMode: SortMode
        get() = SortMode.deserialize(_genreSortMode)
        set(value) {
            _genreSortMode = value.serialize()
        }

    // List-Appearance
    var albumGridSize: Int by IntPref(
        ALBUM_GRID_SIZE, App.instance.resources.getInteger(R.integer.default_grid_columns)
    )
    var songGridSize: Int by IntPref(
        SONG_GRID_SIZE, App.instance.resources.getInteger(R.integer.default_list_columns)
    )
    var artistGridSize: Int by IntPref(
        ARTIST_GRID_SIZE, App.instance.resources.getInteger(R.integer.default_list_columns)
    )
    var genreGridSize: Int by IntPref(
        GENRE_GRID_SIZE, App.instance.resources.getInteger(R.integer.default_list_columns)
    )
    var albumGridSizeLand: Int by IntPref(
        ALBUM_GRID_SIZE_LAND, App.instance.resources.getInteger(R.integer.default_grid_columns_land)
    )
    var songGridSizeLand: Int by IntPref(
        SONG_GRID_SIZE_LAND, App.instance.resources.getInteger(R.integer.default_grid_columns_land)
    )
    var artistGridSizeLand: Int by IntPref(
        ARTIST_GRID_SIZE_LAND, App.instance.resources.getInteger(R.integer.default_grid_columns_land)
    )
    var genreGridSizeLand: Int by IntPref(
        GENRE_GRID_SIZE_LAND, App.instance.resources.getInteger(R.integer.default_grid_columns_land)
    )
    var albumColoredFooters by BooleanPref(ALBUM_COLORED_FOOTERS, true)
    var albumArtistColoredFooters by BooleanPref(ALBUM_ARTIST_COLORED_FOOTERS, true)
    var songColoredFooters by BooleanPref(SONG_COLORED_FOOTERS, true)
    var artistColoredFooters by BooleanPref(ARTIST_COLORED_FOOTERS, true)

    // SleepTimer
    var lastSleepTimerValue: Int by IntPref(LAST_SLEEP_TIMER_VALUE, 30)
    var nextSleepTimerElapsedRealTime: Long by LongPref(NEXT_SLEEP_TIMER_ELAPSED_REALTIME, -1)
    var sleepTimerFinishMusic: Boolean by BooleanPref(SLEEP_TIMER_FINISH_SONG, false)

    // Misc
    var startDirectory: File
        get() = File(
            mPreferences.getString(
                START_DIRECTORY, FoldersFragment.getDefaultStartDirectory().path
            )!!
        )
        set(value) {
            val editor = mPreferences.edit()
            editor.putString(START_DIRECTORY, FileUtil.safeGetCanonicalPath(value))
            editor.apply()
        }
    var initializedBlacklist: Boolean by BooleanPref(INITIALIZED_BLACKLIST, false)
    var ignoreUpgradeVersionCode: Int by IntPref(IGNORE_UPGRADE_VERSION_CODE, 0)

    // Compatibility
    var useLegacyFavoritePlaylistImpl: Boolean by BooleanPref(USE_LEGACY_FAVORITE_PLAYLIST_IMPL, false)

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
        private const val IGNORE_MEDIA_STORE_ARTWORK = "ignore_media_store_artwork"
        private const val AUTO_DOWNLOAD_IMAGES_POLICY = "auto_download_images_policy"

        // Behavior-Playing
        private const val REMEMBER_SHUFFLE = "remember_shuffle"
        private const val AUDIO_DUCKING = "audio_ducking"
        const val GAPLESS_PLAYBACK = "gapless_playback"

        // Behavior-Lyrics
        private const val SYNCHRONIZED_LYRICS_SHOW = "synchronized_lyrics_show"
        private const val DISPLAY_LYRICS_TIME_AXIS = "display_lyrics_time_axis"
        const val BROADCAST_SYNCHRONIZED_LYRICS = "synchronized_lyrics_send"

        // List-Cutoff
        private const val LAST_ADDED_CUTOFF = "last_added_interval"

        // Upgrade
        private const val CHECK_UPGRADE_AT_STARTUP = "check_upgrade_at_startup"

        // List-SortMode
        private const val SONG_SORT_MODE = "song_sort_mode"
        private const val ALBUM_SORT_MODE = "album_sort_mode"
        private const val ARTIST_SORT_MODE = "artist_sort_mode"
        private const val GENRE_SORT_MODE = "genre_sort_mode"

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

        // SleepTimer
        private const val LAST_SLEEP_TIMER_VALUE = "last_sleep_timer_value"
        private const val NEXT_SLEEP_TIMER_ELAPSED_REALTIME = "next_sleep_timer_elapsed_real_time"
        private const val SLEEP_TIMER_FINISH_SONG = "sleep_timer_finish_music"

        // Misc
        private const val START_DIRECTORY = "start_directory"
        private const val INITIALIZED_BLACKLIST = "initialized_blacklist"
        private const val IGNORE_UPGRADE_VERSION_CODE = "ignore_upgrade_version_code"

        // compatibility
        private const val USE_LEGACY_FAVORITE_PLAYLIST_IMPL = "use_legacy_favorite_playlist_impl"

        // Changelog
        private const val LAST_CHANGELOG_VERSION = "last_changelog_version"
        private const val INTRO_SHOWN = "intro_shown"

        // unused & deprecated
        const val FORCE_SQUARE_ALBUM_COVER = "force_square_album_art"

        @Deprecated("removed since version code 101")
        const val LIBRARY_CATEGORIES = "library_categories"

        //
        // Singleton
        //
        private var singleton: Setting? = null
        val instance: Setting
            get() {
                if (singleton == null) singleton = Setting(App.instance)
                return singleton!!
            }

        @JvmStatic
        fun instance(): Setting = instance

        /*
        @JvmStatic
        fun edit(): Setting {
            if (singleton == null) singleton = Setting(App.instance)
            return singleton!!
        }
        */

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

        @StyleRes
        fun getThemeResFromPrefValue(themePrefValue: String?): Int {
            return when (themePrefValue) {
                "dark" -> R.style.Theme_Phonograph_Dark
                "black" -> R.style.Theme_Phonograph_Black
                "light" -> R.style.Theme_Phonograph_Light
                "auto" -> R.style.Theme_Phonograph_Auto
                else -> R.style.Theme_Phonograph_Auto
            }
        }
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
