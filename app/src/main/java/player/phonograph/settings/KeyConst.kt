/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.settings

import androidx.annotation.StringDef


//region Keys

// Appearance
const val THEME = "theme"

const val HOME_TAB_CONFIG = "home_tab_config"
const val COLORED_APP_SHORTCUTS = "colored_app_shortcuts"
const val FIXED_TAB_LAYOUT = "fixed_tab_layout"

const val COLORED_STATUSBAR = "colored_statusbar"
const val COLORED_NAVIGATION_BAR = "colored_navigation_bar"
const val ENABLE_MONET = "enable_monet"
const val SELECTED_PRIMARY_COLOR = "primary_color_selected"
const val SELECTED_ACCENT_COLOR = "accent_color_selected"
const val MONET_PALETTE_PRIMARY_COLOR = "primary_color_monet_palette"
const val MONET_PALETTE_ACCENT_COLOR = "accent_color_monet_palette"

// Appearance - Notification
const val COLORED_NOTIFICATION = "colored_notification"
const val CLASSIC_NOTIFICATION = "classic_notification"
const val NOTIFICATION_ACTIONS = "notification_actions"

// Behavior-Retention
const val REMEMBER_LAST_TAB = "remember_last_tab"
const val LAST_PAGE = "last_start_page"
const val NOW_PLAYING_SCREEN_ID = "now_playing_screen_id"

// Database

// Behavior-File
const val PRELOAD_IMAGES = "preload_images"
const val IMAGE_SOURCE_CONFIG = "image_source_config"
const val IMAGE_CACHE = "image_cache"

// Behavior-Playing
const val SONG_ITEM_CLICK_MODE = "song_item_click_extra_flag"
const val SONG_ITEM_CLICK_EXTRA_FLAG = "song_item_click_extra_mode"
const val AUDIO_DUCKING = "audio_ducking"
const val RESUME_AFTER_AUDIO_FOCUS_GAIN = "resume_after_audio_focus_gain"
const val ALWAYS_PLAY = "always_play"
const val GAPLESS_PLAYBACK = "gapless_playback"
const val ENABLE_LYRICS = "enable_lyrics"
const val BROADCAST_SYNCHRONIZED_LYRICS = "synchronized_lyrics_send"
const val USE_LEGACY_STATUS_BAR_LYRICS_API = "use_legacy_status_bar_lyrics_api"
const val BROADCAST_CURRENT_PLAYER_STATE = "broadcast_current_player_state"
const val PERSISTENT_PLAYBACK_NOTIFICATION = "persistent_playback_notification"

// Behavior-Lyrics
const val SYNCHRONIZED_LYRICS_SHOW = "synchronized_lyrics_show"
const val DISPLAY_LYRICS_TIME_AXIS = "display_lyrics_time_axis"

// List-Cutoff
const val LAST_ADDED_CUTOFF_MODE = "last_added_cutoff_mode"
const val LAST_ADDED_CUTOFF_DURATION = "last_added_cutoff_duration"

// Upgrade
const val CHECK_UPGRADE_AT_STARTUP = "check_upgrade_at_startup"
const val CHECK_UPGRADE_INTERVAL = "check_upgrade_interval"
const val LAST_CHECK_UPGRADE_TIME = "last_check_upgrade_time"

// List-SortMode
const val SONG_SORT_MODE = "song_sort_mode"
const val ALBUM_SORT_MODE = "album_sort_mode"
const val ARTIST_SORT_MODE = "artist_sort_mode"
const val GENRE_SORT_MODE = "genre_sort_mode"

const val FILE_SORT_MODE = "file_sort_mode"
const val SONG_COLLECTION_SORT_MODE = "song_collection_sort_mode"

const val PLAYLIST_SORT_MODE = "playlist_sort_mode"

// List-Appearance
const val ALBUM_ARTIST_COLORED_FOOTERS = "album_artist_colored_footers"
const val ALBUM_COLORED_FOOTERS = "album_colored_footers"
const val SONG_COLORED_FOOTERS = "song_colored_footers"
const val ARTIST_COLORED_FOOTERS = "artist_colored_footers"
const val SHOW_FILE_IMAGINES = "show_file_imagines"

// ListPage-Appearance

const val SONG_ITEM_LAYOUT = "song_item_layout"
const val SONG_ITEM_LAYOUT_LAND = "song_item_layout_land"
const val ALBUM_ITEM_LAYOUT = "album_item_layout"
const val ALBUM_ITEM_LAYOUT_LAND = "album_item_layout_land"
const val ARTIST_ITEM_LAYOUT = "artist_item_layout"
const val ARTIST_ITEM_LAYOUT_LAND = "artist_item_layout_land"

const val ALBUM_GRID_SIZE = "album_grid_size"
const val ALBUM_GRID_SIZE_LAND = "album_grid_size_land"
const val SONG_GRID_SIZE = "song_grid_size"
const val SONG_GRID_SIZE_LAND = "song_grid_size_land"
const val ARTIST_GRID_SIZE = "artist_grid_size"
const val ARTIST_GRID_SIZE_LAND = "artist_grid_size_land"
const val GENRE_GRID_SIZE = "genre_grid_size"
const val GENRE_GRID_SIZE_LAND = "genre_grid_size_land"
const val PLAYLIST_GRID_SIZE = "playlist_grid_size"
const val PLAYLIST_GRID_SIZE_LAND = "playlist_grid_size_land"


// SleepTimer
const val LAST_SLEEP_TIMER_VALUE = "last_sleep_timer_value"
const val NEXT_SLEEP_TIMER_ELAPSED_REALTIME = "next_sleep_timer_elapsed_real_time"
const val SLEEP_TIMER_FINISH_SONG = "sleep_timer_finish_music"

// Misc
const val IGNORE_UPGRADE_DATE = "ignore_upgrade_date"
const val PATH_FILTER_EXCLUDE_MODE = "path_filter_exclude_mode"

// compatibility
const val USE_LEGACY_FAVORITE_PLAYLIST_IMPL = "use_legacy_favorite_playlist_impl"
const val USE_LEGACY_LIST_FILES_IMPL = "use_legacy_list_files_impl"
const val PLAYLIST_FILES_OPERATION_BEHAVIOUR = "playlist_files_operation_behaviour"
const val USE_LEGACY_DETAIL_DIALOG = "use_legacy_detail_dialog"
const val DISABLE_REAL_TIME_SEARCH = "disable_real_time_search"

// unused & deprecated

const val INITIALIZED_BLACKLIST = "initialized_blacklist"
const val PREVIOUS_VERSION = "last_changelog_version"
const val FORCE_SQUARE_ALBUM_COVER = "force_square_album_art"
const val IGNORE_UPGRADE_VERSION_CODE = "ignore_upgrade_version_code"
const val IGNORE_MEDIA_STORE_ARTWORK = "ignore_media_store_artwork"
//endregion

//region Values
// StringArrayPref
const val PLAYLIST_OPS_BEHAVIOUR_AUTO = "auto"
const val PLAYLIST_OPS_BEHAVIOUR_FORCE_SAF = "force_saf"
const val PLAYLIST_OPS_BEHAVIOUR_FORCE_LEGACY = "force_legacy"
// Theme
const val THEME_AUTO_LIGHTBLACK = "auto_lightblack"
const val THEME_AUTO_LIGHTDARK = "auto_lightdark"
const val THEME_LIGHT = "light"
const val THEME_BLACK = "black"
const val THEME_DARK = "dark"

@StringDef(THEME_AUTO_LIGHTBLACK, THEME_AUTO_LIGHTDARK, THEME_LIGHT, THEME_BLACK, THEME_DARK)
@Retention(AnnotationRetention.SOURCE)
annotation class GeneralTheme
//endregion