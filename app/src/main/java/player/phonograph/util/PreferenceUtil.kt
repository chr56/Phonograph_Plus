/*
 * Copyright (c) 2021 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.Toast
import androidx.annotation.StyleRes
import androidx.preference.PreferenceManager
import chr_56.MDthemer.core.ThemeColor
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import player.phonograph.App
import player.phonograph.R
import player.phonograph.helper.SortOrder
import player.phonograph.model.CategoryInfo
import player.phonograph.ui.fragments.mainactivity.folders.FoldersFragment
import player.phonograph.ui.fragments.player.NowPlayingScreen
import java.io.File
import java.util.ArrayList

@SuppressLint("ApplySharedPref")
class PreferenceUtil(context: Context) {

//    private var sInstance: PreferenceUtil? = null
    private val mPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

//    fun getInstance(context: Context): PreferenceUtil {
//        return if (sInstance == null) PreferenceUtil(context.applicationContext)
//        else sInstance!!
//    }

//    fun isAllowedToDownloadMetadata(context: Context): Boolean {
//        return when (getInstance(context).autoDownloadImagesPolicy()) {
//            "always" -> true
//            "only_wifi" -> {
//                val connectivityManager =
//                    context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
//                val netInfo = connectivityManager.activeNetworkInfo
//                netInfo != null && netInfo.type == ConnectivityManager.TYPE_WIFI && netInfo.isConnectedOrConnecting
//            }
//            "never" -> false
//            else -> false
//        }
//    }

    fun registerOnSharedPreferenceChangedListener(sharedPreferenceChangeListener: OnSharedPreferenceChangeListener?) {
        mPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener)
    }

    fun unregisterOnSharedPreferenceChangedListener(sharedPreferenceChangeListener: OnSharedPreferenceChangeListener?) {
        mPreferences.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener)
    }

//    @StyleRes
//    fun getGeneralTheme(): Int {
//        return getThemeResFromPrefValue(
//            mPreferences.getString(GENERAL_THEME, "auto")
//        )
//    }
//    fun setGeneralTheme(theme: String?) {
//        val editor = mPreferences.edit()
//        editor.putString(GENERAL_THEME, theme)
//        editor.commit()
//    }

    @get:StyleRes
    val generalTheme: Int
        get() = getThemeResFromPrefValue(
            mPreferences.getString(GENERAL_THEME, "auto")
        )
    fun setGeneralTheme(theme: String?) {
        val editor = mPreferences.edit()
        editor.putString(GENERAL_THEME, theme)
        editor.commit()
    }

    fun rememberLastTab(): Boolean {
        return mPreferences.getBoolean(REMEMBER_LAST_TAB, true)
    }

    var lastPage: Int
        get() = mPreferences.getInt(LAST_PAGE, 0)
        set(value) {
            val editor = mPreferences.edit()
            editor.putInt(LAST_PAGE, value)
            editor.apply()
        }

    var lastMusicChooser: Int
        get() = mPreferences.getInt(LAST_MUSIC_CHOOSER, 0)
        set(value) {
            val editor = mPreferences.edit()
            editor.putInt(LAST_MUSIC_CHOOSER, value)
            editor.apply()
        }

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
            editor.commit()
        }

    fun coloredNotification(): Boolean {
        return mPreferences.getBoolean(COLORED_NOTIFICATION, true)
    }

    fun classicNotification(): Boolean {
        return mPreferences.getBoolean(CLASSIC_NOTIFICATION, false)
    }

    fun setColoredNotification(value: Boolean) {
        val editor = mPreferences.edit()
        editor.putBoolean(COLORED_NOTIFICATION, value)
        editor.apply()
    }

    fun setClassicNotification(value: Boolean) {
        val editor = mPreferences.edit()
        editor.putBoolean(CLASSIC_NOTIFICATION, value)
        editor.apply()
    }

    fun setColoredAppShortcuts(value: Boolean) {
        val editor = mPreferences.edit()
        editor.putBoolean(COLORED_APP_SHORTCUTS, value)
        editor.apply()
    }

    fun coloredAppShortcuts(): Boolean {
        return mPreferences.getBoolean(COLORED_APP_SHORTCUTS, true)
    }

    fun gaplessPlayback(): Boolean {
        return mPreferences.getBoolean(GAPLESS_PLAYBACK, false)
    }

    fun audioDucking(): Boolean {
        return mPreferences.getBoolean(AUDIO_DUCKING, true)
    }

    fun albumArtOnLockscreen(): Boolean {
        return mPreferences.getBoolean(ALBUM_ART_ON_LOCKSCREEN, true)
    }

    fun blurredAlbumArt(): Boolean {
        return mPreferences.getBoolean(BLURRED_ALBUM_ART, false)
    }

    fun ignoreMediaStoreArtwork(): Boolean {
        return mPreferences.getBoolean(IGNORE_MEDIA_STORE_ARTWORK, false)
    }

    var artistSortOrder: String
        get() = mPreferences.getString(ARTIST_SORT_ORDER, SortOrder.ArtistSortOrder.ARTIST_A_Z)!!
        set(sortOrder) {
            val editor = mPreferences.edit()
            editor.putString(ARTIST_SORT_ORDER, sortOrder)
            editor.commit()
        }

    val artistSongSortOrder: String
        get() = mPreferences.getString(
            ARTIST_SONG_SORT_ORDER, SortOrder.ArtistSongSortOrder.SONG_A_Z
        )!!

    val artistAlbumSortOrder: String
        get() = mPreferences.getString(
            ARTIST_ALBUM_SORT_ORDER, SortOrder.ArtistAlbumSortOrder.ALBUM_YEAR
        )!!

    var albumSortOrder: String
        get() = mPreferences.getString(ALBUM_SORT_ORDER, SortOrder.AlbumSortOrder.ALBUM_A_Z)!!
        set(sortOrder) {
            val editor = mPreferences.edit()
            editor.putString(ALBUM_SORT_ORDER, sortOrder)
            editor.commit()
        }

    val albumSongSortOrder: String
        get() = mPreferences.getString(
            ALBUM_SONG_SORT_ORDER, SortOrder.AlbumSongSortOrder.SONG_TRACK_LIST
        )!!

    var songSortOrder: String
        get() = mPreferences.getString(SONG_SORT_ORDER, SortOrder.SongSortOrder.SONG_A_Z)!!
        set(sortOrder) {
            val editor = mPreferences.edit()
            editor.putString(SONG_SORT_ORDER, sortOrder)
            editor.commit()
        }

    val genreSortOrder: String
        get() = mPreferences.getString(GENRE_SORT_ORDER, SortOrder.GenreSortOrder.GENRE_A_Z)!!

    val lastAddedCutoff: Long
        get() {
            val calendarUtil = CalendarUtil()
            val interval: Long = when (mPreferences.getString(LAST_ADDED_CUTOFF, "")) {
                "today" -> calendarUtil.elapsedToday
                "this_week" -> calendarUtil.elapsedWeek
                "past_seven_days" -> calendarUtil.getElapsedDays(7)
                "past_three_months" -> calendarUtil.getElapsedMonths(3)
                "this_year" -> calendarUtil.elapsedYear
                "this_month" -> calendarUtil.elapsedMonth
                else -> calendarUtil.elapsedMonth
            }
            return (System.currentTimeMillis() - interval) / 1000
        }

    var lastSleepTimerValue: Int
        get() = mPreferences.getInt(LAST_SLEEP_TIMER_VALUE, 30)
        set(value) {
            val editor = mPreferences.edit()
            editor.putInt(LAST_SLEEP_TIMER_VALUE, value)
            editor.apply()
        }

    var nextSleepTimerElapsedRealTime: Long
        get() = mPreferences.getLong(NEXT_SLEEP_TIMER_ELAPSED_REALTIME, -1)
        set(value) {
            val editor = mPreferences.edit()
            editor.putLong(NEXT_SLEEP_TIMER_ELAPSED_REALTIME, value)
            editor.apply()
        }

    var sleepTimerFinishMusic: Boolean
        get() = mPreferences.getBoolean(SLEEP_TIMER_FINISH_SONG, false)
        set(value) {
            val editor = mPreferences.edit()
            editor.putBoolean(SLEEP_TIMER_FINISH_SONG, value)
            editor.apply()
        }

    var albumGridSize: Int
        get() {
            return mPreferences.getInt(
                ALBUM_GRID_SIZE,
                App.instance.resources.getInteger(R.integer.default_grid_columns)
            )
        }
        set(gridSize) {
            val editor = mPreferences.edit()
            editor.putInt(ALBUM_GRID_SIZE, gridSize)
            editor.apply()
        }

    var songGridSize: Int
        get() {
            return mPreferences.getInt(
                SONG_GRID_SIZE,
                App.instance.resources.getInteger(R.integer.default_list_columns)
            )
        }
        set(gridSize) {
            val editor = mPreferences.edit()
            editor.putInt(SONG_GRID_SIZE, gridSize)
            editor.apply()
        }

    var artistGridSize: Int
        get() {
            return mPreferences.getInt(
                ARTIST_GRID_SIZE,
                App.instance.resources.getInteger(R.integer.default_list_columns)
            )
        }
        set(gridSize) {
            val editor = mPreferences.edit()
            editor.putInt(ARTIST_GRID_SIZE, gridSize)
            editor.apply()
        }

    var albumGridSizeLand: Int
        get() {
            return mPreferences.getInt(
                ALBUM_GRID_SIZE_LAND,
                App.instance.resources.getInteger(R.integer.default_grid_columns_land)
            )
        }
        set(gridSize) {
            val editor = mPreferences.edit()
            editor.putInt(ALBUM_GRID_SIZE_LAND, gridSize)
            editor.apply()
        }
    var songGridSizeLand: Int
        get() {
            return mPreferences.getInt(
                SONG_GRID_SIZE_LAND,
                App.instance.resources.getInteger(R.integer.default_grid_columns_land)
            )
        }
        set(gridSize) {
            val editor = mPreferences.edit()
            editor.putInt(SONG_GRID_SIZE_LAND, gridSize)
            editor.apply()
        }
    var artistGridSizeLand: Int
        get() {
            return mPreferences.getInt(
                ARTIST_GRID_SIZE_LAND,
                App.instance.resources.getInteger(R.integer.default_grid_columns_land)
            )
        }
        set(gridSize) {
            val editor = mPreferences.edit()
            editor.putInt(ARTIST_GRID_SIZE_LAND, gridSize)
            editor.apply()
        }

    fun setAlbumColoredFooters(value: Boolean) {
        val editor = mPreferences.edit()
        editor.putBoolean(ALBUM_COLORED_FOOTERS, value)
        editor.apply()
    }

    fun albumColoredFooters(): Boolean {
        return mPreferences.getBoolean(ALBUM_COLORED_FOOTERS, true)
    }

    fun setAlbumArtistColoredFooters(value: Boolean) {
        val editor = mPreferences.edit()
        editor.putBoolean(ALBUM_ARTIST_COLORED_FOOTERS, value)
        editor.apply()
    }

    fun albumArtistColoredFooters(): Boolean {
        return mPreferences.getBoolean(ALBUM_ARTIST_COLORED_FOOTERS, true)
    }

    fun setSongColoredFooters(value: Boolean) {
        val editor = mPreferences.edit()
        editor.putBoolean(SONG_COLORED_FOOTERS, value)
        editor.apply()
    }

    fun songColoredFooters(): Boolean {
        return mPreferences.getBoolean(SONG_COLORED_FOOTERS, true)
    }

    fun setArtistColoredFooters(value: Boolean) {
        val editor = mPreferences.edit()
        editor.putBoolean(ARTIST_COLORED_FOOTERS, value)
        editor.apply()
    }

    fun artistColoredFooters(): Boolean {
        return mPreferences.getBoolean(ARTIST_COLORED_FOOTERS, true)
    }

    fun setLastChangeLogVersion(version: Int) {
        mPreferences.edit().putInt(LAST_CHANGELOG_VERSION, version).apply()
    }

    fun getLastChangelogVersion(): Int {
        return mPreferences.getInt(LAST_CHANGELOG_VERSION, -1)
    }

    fun setIntroShown() {
        // don't use apply here
        mPreferences.edit().putBoolean(INTRO_SHOWN, true).commit()
    }

    fun introShown(): Boolean {
        return mPreferences.getBoolean(INTRO_SHOWN, false)
    }

    fun rememberShuffle(): Boolean {
        return mPreferences.getBoolean(REMEMBER_SHUFFLE, true)
    }

    fun fixedTabLayout(): Boolean {
        return mPreferences.getBoolean(FIXED_TAB_LAYOUT, false)
    }

    fun autoDownloadImagesPolicy(): String? {
        return mPreferences.getString(AUTO_DOWNLOAD_IMAGES_POLICY, "only_wifi")
    }

    fun getStartDirectory(): File {
        return File(
            mPreferences.getString(
                START_DIRECTORY,
                FoldersFragment.getDefaultStartDirectory().path
            )!!
        )
    }

    fun setStartDirectory(file: File?) {
        val editor = mPreferences.edit()
        editor.putString(START_DIRECTORY, FileUtil.safeGetCanonicalPath(file))
        editor.apply()
    }

    fun displaySynchronizedLyricsTimeAxis(): Boolean =
        mPreferences.getBoolean(DISPLAY_LYRICS_TIME_AXIS, true)

    fun synchronizedLyricsShow(): Boolean {
        return mPreferences.getBoolean(SYNCHRONIZED_LYRICS_SHOW, true)
    }

    fun broadcastSynchronizedLyrics(): Boolean =
        mPreferences.getBoolean(BROADCAST_SYNCHRONIZED_LYRICS, true)

    fun setInitializedBlacklist() {
        val editor = mPreferences.edit()
        editor.putBoolean(INITIALIZED_BLACKLIST, true)
        editor.apply()
    }

    fun initializedBlacklist(): Boolean {
        return mPreferences.getBoolean(INITIALIZED_BLACKLIST, false)
    }

    var libraryCategoryInfos: List<CategoryInfo>?
        get() {
            val data = mPreferences.getString(LIBRARY_CATEGORIES, null)
            if (data != null) {
                val gson = Gson()
                val collectionType = object : TypeToken<List<CategoryInfo?>?>() {}.type
                try {
                    return gson.fromJson<List<CategoryInfo>>(data, collectionType)
                } catch (e: JsonSyntaxException) {
                    e.printStackTrace()
                }
            }
            return defaultLibraryCategoryInfos
        }
        set(categories) {
            val gson = Gson()
            val collectionType = object : TypeToken<List<CategoryInfo?>?>() {}.type
            val editor = mPreferences.edit()
            editor.putString(LIBRARY_CATEGORIES, gson.toJson(categories, collectionType))
            editor.apply()
        }
    val defaultLibraryCategoryInfos: List<CategoryInfo>
        get() {
            val defaultCategoryInfos: MutableList<CategoryInfo> = ArrayList(5)
            defaultCategoryInfos.add(CategoryInfo(CategoryInfo.Category.SONGS, true))
            defaultCategoryInfos.add(CategoryInfo(CategoryInfo.Category.ALBUMS, true))
            defaultCategoryInfos.add(CategoryInfo(CategoryInfo.Category.ARTISTS, true))
            defaultCategoryInfos.add(CategoryInfo(CategoryInfo.Category.GENRES, true))
            defaultCategoryInfos.add(CategoryInfo(CategoryInfo.Category.PLAYLISTS, true))
            return defaultCategoryInfos
        }

    var checkUpgradeAtStartup: Boolean
        get() = mPreferences.getBoolean(CHECK_UPGRADE_AT_STARTUP, true)
        set(value) {
            mPreferences.edit().putBoolean(CHECK_UPGRADE_AT_STARTUP, value).apply()
        }

    /**
     * **Dangerous !**, this reset all SharedPreferences!
     * @param context this is used to make toast
     */
    @SuppressLint("ApplySharedPref") // must do immediately!
    fun clearAllPreference(context: Context) {
        mPreferences.edit().clear().commit()

        // lib
        ThemeColor.editTheme(context.applicationContext).clearAllPreference()
        Toast.makeText(context, R.string.success, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private var sInstance: PreferenceUtil? = null
        @JvmStatic
        fun getInstance(context: Context): PreferenceUtil {
            if (sInstance == null) {
                sInstance = PreferenceUtil(context.applicationContext)
            }
            return sInstance as PreferenceUtil
        }

        @JvmStatic
        fun isAllowedToDownloadMetadata(context: Context): Boolean {
            return when (getInstance(context).autoDownloadImagesPolicy()) {
                "always" -> true
                "only_wifi" -> {
                    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

                    if (!cm.isActiveNetworkMetered) return false // we pass first metred Wifi and Cellular
                    val network = cm.activeNetwork ?: return false // no active network?
                    val capabilities = cm.getNetworkCapabilities(network) ?: return false // no capabilities?
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

        const val GENERAL_THEME = "general_theme"
        const val REMEMBER_LAST_TAB = "remember_last_tab"
        const val LAST_PAGE = "last_start_page"
        const val LAST_MUSIC_CHOOSER = "last_music_chooser"
        const val NOW_PLAYING_SCREEN_ID = "now_playing_screen_id"

        const val ARTIST_SORT_ORDER = "artist_sort_order"
        const val ARTIST_SONG_SORT_ORDER = "artist_song_sort_order"
        const val ARTIST_ALBUM_SORT_ORDER = "artist_album_sort_order"
        const val ALBUM_SORT_ORDER = "album_sort_order"
        const val ALBUM_SONG_SORT_ORDER = "album_song_sort_order"
        const val SONG_SORT_ORDER = "song_sort_order"
        const val GENRE_SORT_ORDER = "genre_sort_order"

        const val ALBUM_GRID_SIZE = "album_grid_size"
        const val ALBUM_GRID_SIZE_LAND = "album_grid_size_land"

        const val SONG_GRID_SIZE = "song_grid_size"
        const val SONG_GRID_SIZE_LAND = "song_grid_size_land"

        const val ARTIST_GRID_SIZE = "artist_grid_size"
        const val ARTIST_GRID_SIZE_LAND = "artist_grid_size_land"

        const val ALBUM_COLORED_FOOTERS = "album_colored_footers"
        const val SONG_COLORED_FOOTERS = "song_colored_footers"
        const val ARTIST_COLORED_FOOTERS = "artist_colored_footers"
        const val ALBUM_ARTIST_COLORED_FOOTERS = "album_artist_colored_footers"

        const val FORCE_SQUARE_ALBUM_COVER = "force_square_album_art"

        const val COLORED_NOTIFICATION = "colored_notification"
        const val CLASSIC_NOTIFICATION = "classic_notification"

        const val COLORED_APP_SHORTCUTS = "colored_app_shortcuts"

        const val AUDIO_DUCKING = "audio_ducking"
        const val GAPLESS_PLAYBACK = "gapless_playback"

        const val LAST_ADDED_CUTOFF = "last_added_interval"

        const val ALBUM_ART_ON_LOCKSCREEN = "album_art_on_lockscreen"
        const val BLURRED_ALBUM_ART = "blurred_album_art"

        const val LAST_SLEEP_TIMER_VALUE = "last_sleep_timer_value"
        const val NEXT_SLEEP_TIMER_ELAPSED_REALTIME = "next_sleep_timer_elapsed_real_time"
        const val SLEEP_TIMER_FINISH_SONG = "sleep_timer_finish_music"

        const val IGNORE_MEDIA_STORE_ARTWORK = "ignore_media_store_artwork"

        const val LAST_CHANGELOG_VERSION = "last_changelog_version"
        const val INTRO_SHOWN = "intro_shown"

        const val AUTO_DOWNLOAD_IMAGES_POLICY = "auto_download_images_policy"

        const val START_DIRECTORY = "start_directory"

        const val DISPLAY_LYRICS_TIME_AXIS = "display_lyrics_time_axis"

        const val SYNCHRONIZED_LYRICS_SHOW = "synchronized_lyrics_show"

        const val BROADCAST_SYNCHRONIZED_LYRICS = "synchronized_lyrics_send"

        const val INITIALIZED_BLACKLIST = "initialized_blacklist"

        const val LIBRARY_CATEGORIES = "library_categories"

        private const val REMEMBER_SHUFFLE = "remember_shuffle"

        const val FIXED_TAB_LAYOUT = "fixed_tab_layout"

        const val CHECK_UPGRADE_AT_STARTUP = "check_upgrade_at_startup"
    }
}
