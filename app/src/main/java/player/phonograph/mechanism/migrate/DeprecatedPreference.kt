/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.mechanism.migrate

object DeprecatedPreference {

    // LibraryCategories: "removed since version code 101"
    const val LIBRARY_CATEGORIES = "library_categories"


    // SortOrder: "removed since version code 210"
    const val ARTIST_SORT_ORDER = "artist_sort_order"
    const val ARTIST_SONG_SORT_ORDER = "artist_song_sort_order"
    const val ARTIST_ALBUM_SORT_ORDER = "artist_album_sort_order"
    const val ALBUM_SORT_ORDER = "album_sort_order"
    const val ALBUM_SONG_SORT_ORDER = "album_song_sort_order"
    const val SONG_SORT_ORDER = "song_sort_order"
    const val GENRE_SORT_ORDER = "genre_sort_order"


    // MusicChooserPreference: "removed since version code 262"
    const val LAST_MUSIC_CHOOSER = "last_music_chooser"


    // LegacyClickPreference: "removed since version code 402"
    const val REMEMBER_SHUFFLE = "remember_shuffle"
    const val KEEP_PLAYING_QUEUE_INTACT = "keep_playing_queue_intact"


    // QueueConfiguration: "move to a separate preference since 460"
    const val PREF_POSITION = "POSITION"
    const val PREF_SHUFFLE_MODE = "SHUFFLE_MODE"
    const val PREF_REPEAT_MODE = "REPEAT_MODE"
    const val PREF_POSITION_IN_TRACK = "POSITION_IN_TRACK"


    // LockScreenCover: "remove lockscreen cover since 522"
    const val ALBUM_ART_ON_LOCKSCREEN = "album_art_on_lockscreen"
    const val BLURRED_ALBUM_ART = "blurred_album_art"


    // AutoDownloadMetadata: "removed Auto Download Metadata from last.fm since version code 1011"
    const val AUTO_DOWNLOAD_IMAGES_POLICY = "auto_download_images_policy"
    const val DOWNLOAD_IMAGES_POLICY_ALWAYS = "always"
    const val DOWNLOAD_IMAGES_POLICY_ONLY_WIFI = "only_wifi"
    const val DOWNLOAD_IMAGES_POLICY_NEVER = "never"


    // LegacyLastAddedCutoffInterval: "replaced with the flexible one since version code 1011"
    const val LEGACY_LAST_ADDED_CUTOFF = "last_added_interval"
    const val INTERVAL_TODAY = "today"
    const val INTERVAL_PAST_SEVEN_DAYS = "past_seven_days"
    const val INTERVAL_PAST_FOURTEEN_DAYS = "past_fourteen_days"
    const val INTERVAL_PAST_ONE_MONTH = "past_one_month"
    const val INTERVAL_PAST_THREE_MONTHS = "past_three_months"
    const val INTERVAL_THIS_WEEK = "this_week"
    const val INTERVAL_THIS_MONTH = "this_month"
    const val INTERVAL_THIS_YEAR = "this_year"


    // ThemeColorKeys: "migrate to datastore since version code 1064"
    const val THEME_CONFIG_PREFERENCE_NAME = "theme_color_cfg"
    const val KEY_IS_CONFIGURED = "is_configured"
    const val KEY_VERSION = "is_configured_version"
    const val KEY_LAST_EDIT_TIME = "values_changed"
    const val KEY_PRIMARY_COLOR = "primary_color"
    const val KEY_ACCENT_COLOR = "accent_color"
    const val KEY_COLORED_STATUSBAR = "apply_primarydark_statusbar"
    const val KEY_COLORED_NAVIGATION_BAR = "apply_primary_navbar"
    const val KEY_ENABLE_MONET = "enable_monet"
    const val KEY_MONET_PRIMARY_COLOR = "monet_primary_color"
    const val KEY_MONET_ACCENT_COLOR = "monet_accent_color"


    // StyleConfigKeys: "migrate to datastore since version code 1064"
    const val PREFERENCE_NAME = "style_config"
    const val KEY_THEME = "theme"


    // LegacyDetailDialog: "remove fallback since 1081"
    const val USE_LEGACY_DETAIL_DIALOG = "use_legacy_detail_dialog"


    // PlaylistFilesOperationBehaviour: "removed since 1085"
    const val PLAYLIST_FILES_OPERATION_BEHAVIOUR = "playlist_files_operation_behaviour"


    // ColoredSystemBars: "removed since 1086"
    const val COLORED_STATUSBAR = "colored_statusbar"
    const val COLORED_NAVIGATION_BAR = "colored_navigation_bar"


    // PreloadImages: "remove since 1100"
    const val PRELOAD_IMAGES = "preload_images"


    // NowPlayingScreen: "refactored since 1100"
    const val NOW_PLAYING_SCREEN_ID = "now_playing_screen_id"

}