/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.ui.resource

import player.phonograph.R
import player.phonograph.model.menu.ActionSemantic
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete


object ActionSemantics {

    //region General
    val More: ActionSemantic
        get() = ConcreteActionSemantic(
            textRes = R.string.action_more,
            imageRes = R.drawable.ic_more_vert_white_24dp,
            imageVector = Icons.Default.MoreVert,
        )

    val Add: ActionSemantic
        get() = ConcreteActionSemantic(
            textRes = R.string.action_add,
            imageRes = R.drawable.ic_add_white_24dp,
            imageVector = Icons.Default.Add,
        )

    val Edit: ActionSemantic
        get() = ConcreteActionSemantic(
            textRes = R.string.action_edit,
            imageRes = R.drawable.ic_edit_white_24dp,
            imageVector = Icons.Default.Edit,
        )

    val Confirm: ActionSemantic
        get() = ConcreteActionSemantic(
            textRes = android.R.string.ok,
            imageRes = R.drawable.ic_check,
            imageVector = Icons.Default.Check,
        )

    val Exit: ActionSemantic
        get() = ConcreteActionSemantic(
            textRes = R.string.action_exit,
            imageRes = R.drawable.ic_nav_back_white_24dp,
            imageVector = Icons.AutoMirrored.Filled.ArrowBack
        )

    val Refresh: ActionSemantic
        get() = ConcreteActionSemantic(
            textRes = R.string.action_refresh,
            imageRes = R.drawable.ic_refresh_white_24dp,
            imageVector = Icons.Default.Refresh,
        )

    val Search: ActionSemantic
        get() = ConcreteActionSemantic(
            textRes = R.string.action_search,
            imageRes = R.drawable.ic_search_white_24dp,
            imageVector = Icons.Default.Search,
        )

    //endregion

    //region Playback
    val Play: ActionSemantic
        get() = ConcreteActionSemantic(
            textRes = R.string.action_play,
            imageRes = R.drawable.ic_play_arrow_white_24dp,
            imageVector = Icons.Default.PlayArrow,
        )

    val PlayNext: ActionSemantic
        get() = ConcreteActionSemantic(
            textRes = R.string.action_play_next,
            imageRes = R.drawable.ic_redo_white_24dp,
        )

    val AddToPlayingQueue: ActionSemantic
        get() = ConcreteActionSemantic(
            textRes = R.string.action_add_to_playing_queue,
            imageRes = R.drawable.ic_library_add_white_24dp,
        )

    val RemoveFromPlayingQueue: ActionSemantic
        get() = ConcreteActionSemantic(
            textRes = R.string.action_remove_from_playing_queue,
        )

    val AddToPlaylist: ActionSemantic
        get() = ConcreteActionSemantic(
            textRes = R.string.action_add_to_playlist,
            imageRes = R.drawable.ic_playlist_add_white_24dp,
        )

    val RemoveFromPlaylist: ActionSemantic
        get() = ConcreteActionSemantic(
            textRes = R.string.action_remove_from_playlist,
        )

    val ShuffleAll: ActionSemantic
        get() = ConcreteActionSemantic(
            textRes = R.string.action_shuffle_all,
            imageRes = R.drawable.ic_shuffle_white_24dp,
        )

    val ShuffleAlbum: ActionSemantic
        get() = ConcreteActionSemantic(
            textRes = R.string.action_shuffle_album,
            imageRes = R.drawable.ic_shuffle_white_24dp,
        )

    val ShuffleArtist: ActionSemantic
        get() = ConcreteActionSemantic(
            textRes = R.string.action_shuffle_artist,
            imageRes = R.drawable.ic_shuffle_white_24dp,
        )

    val ShufflePlaylist: ActionSemantic
        get() = ConcreteActionSemantic(
            textRes = R.string.action_shuffle_playlist,
            imageRes = R.drawable.ic_shuffle_white_24dp,
        )

    val ShuffleMode: ActionSemantic
        get() = ConcreteActionSemantic(
            textRes = R.string.action_shuffle_mode,
            imageRes = R.drawable.ic_shuffle_white_24dp,
        )

    val RepeatMode: ActionSemantic
        get() = ConcreteActionSemantic(
            textRes = R.string.action_repeat_mode,
            imageRes = R.drawable.ic_repeat_white_24dp,
        )

    val SetAsRingtone: ActionSemantic
        get() = ConcreteActionSemantic(
            textRes = R.string.action_set_as_ringtone,
        )

    val ClearPlayingQueue: ActionSemantic
        get() = ConcreteActionSemantic(
            textRes = R.string.action_clear_playing_queue,
        )

    val SavePlayingQueue: ActionSemantic
        get() = ConcreteActionSemantic(
            textRes = R.string.action_save_playing_queue,
            imageRes = R.drawable.ic_save_white_24dp,
        )

    //endregion

    //region Navigation
    val GoToArtist: ActionSemantic
        get() = ConcreteActionSemantic(
            textRes = R.string.action_go_to_artist,
            imageRes = R.drawable.ic_person_white_24dp,
            imageVector = Icons.Default.Person,
        )

    val GoToAlbum: ActionSemantic
        get() = ConcreteActionSemantic(
            textRes = R.string.action_go_to_album,
            imageRes = R.drawable.ic_album_white_24dp,
        )

    val Details: ActionSemantic
        get() = ConcreteActionSemantic(
            textRes = R.string.action_details,
            imageRes = R.drawable.ic_info_outline_white_24dp,
            imageVector = Icons.Default.Info,
        )

    val TagEditor: ActionSemantic
        get() = ConcreteActionSemantic(
            textRes = R.string.action_tag_editor,
            imageRes = R.drawable.ic_library_music_white_24dp,
        )

    val SetArtistImage: ActionSemantic
        get() = ConcreteActionSemantic(
            textRes = R.string.action_set_artist_image,
            imageRes = R.drawable.ic_person_white_24dp,
            imageVector = Icons.Default.Person,
        )

    val ResetArtistImage: ActionSemantic
        get() = ConcreteActionSemantic(
            textRes = R.string.action_reset_artist_image,
            imageRes = R.drawable.ic_close_white_24dp,
            imageVector = Icons.Default.Close,
        )

    val SearchOnline: ActionSemantic
        get() = ConcreteActionSemantic(
            textRes = R.string.action_search_online,
            imageRes = R.drawable.ic_open_in_browser_white_24dp,
        )

    val About: ActionSemantic
        get() = ConcreteActionSemantic(
            textRes = R.string.action_about,
            imageRes = R.drawable.ic_help_white_24dp,
            imageVector = Icons.Default.Info,
        )

    val Settings: ActionSemantic
        get() = ConcreteActionSemantic(
            textRes = R.string.action_settings,
            imageRes = R.drawable.ic_settings_white_24dp,
            imageVector = Icons.Default.Settings,
        )

    //endregion

    //region Editing
    val Rename: ActionSemantic
        get() = ConcreteActionSemantic(
            textRes = R.string.action_rename,
            imageRes = R.drawable.ic_edit_white_24dp,
            imageVector = Icons.Default.Edit,
        )

    val Delete: ActionSemantic
        get() = ConcreteActionSemantic(
            textRes = R.string.action_delete,
            imageRes = R.drawable.ic_delete_white_24dp,
            imageVector = Icons.Default.Delete,
        )

    val DeleteFromDevice: ActionSemantic
        get() = ConcreteActionSemantic(
            textRes = R.string.action_delete_from_device,
            imageRes = R.drawable.ic_delete_white_24dp,
            imageVector = Icons.Default.Delete,
        )

    val Clear: ActionSemantic
        get() = ConcreteActionSemantic(
            textRes = R.string.action_clear,
            imageRes = R.drawable.ic_cleaning_bucket_24dp,
            imageVector = Icons.Outlined.Delete,
        )

    val Clean: ActionSemantic
        get() = ConcreteActionSemantic(
            textRes = R.string.action_clean,
            imageRes = R.drawable.ic_cleaning_bucket_24dp,
            imageVector = Icons.Outlined.Delete,
        )

    val CleanMissingItems: ActionSemantic
        get() = ConcreteActionSemantic(
            textRes = R.string.action_clean_missing_items,
            imageRes = R.drawable.ic_cleaning_bucket_24dp,
            imageVector = Icons.Outlined.Delete,
        )

    val Reset: ActionSemantic
        get() = ConcreteActionSemantic(
            textRes = R.string.action_reset,
            imageRes = R.drawable.ic_restore_white_24dp,
        )

    val Save: ActionSemantic
        get() = ConcreteActionSemantic(
            textRes = R.string.action_save,
            imageRes = R.drawable.ic_save_white_24dp,
        )

    val SavePlaylist: ActionSemantic
        get() = ConcreteActionSemantic(
            textRes = R.string.action_save_playlist,
            imageRes = R.drawable.ic_save_white_24dp,
        )

    val SavePlaylists: ActionSemantic
        get() = ConcreteActionSemantic(
            textRes = R.string.action_save_playlists,
            imageRes = R.drawable.ic_save_white_24dp,
        )

    val Select: ActionSemantic
        get() = ConcreteActionSemantic(
            textRes = R.string.action_select,
            imageRes = R.drawable.ic_check,
            imageVector = Icons.Default.Check,
        )

    val SelectAll: ActionSemantic
        get() = ConcreteActionSemantic(
            textRes = R.string.action_select_all,
            imageRes = R.drawable.ic_select_all_white_24dp,
        )

    val UnselectAll: ActionSemantic
        get() = ConcreteActionSemantic(
            textRes = R.string.action_unselect_all,
        )

    val InvertSelection: ActionSemantic
        get() = ConcreteActionSemantic(
            textRes = R.string.action_invert_selection,
        )

    val Pin: ActionSemantic
        get() = ConcreteActionSemantic(
            textRes = R.string.action_pin,
            imageRes = R.drawable.ic_pin_white_24dp,
        )

    val Unpin: ActionSemantic
        get() = ConcreteActionSemantic(
            textRes = R.string.action_unpin,
            imageRes = R.drawable.ic_pin_white_24dp,
        )

    val MoveToTop: ActionSemantic
        get() = ConcreteActionSemantic(
            textRes = R.string.action_move_to_top,
        )

    val MoveUp: ActionSemantic
        get() = ConcreteActionSemantic(
            textRes = R.string.action_move_up,
        )

    val MoveDown: ActionSemantic
        get() = ConcreteActionSemantic(
            textRes = R.string.action_move_down,
        )

    val MoveToBottom: ActionSemantic
        get() = ConcreteActionSemantic(
            textRes = R.string.action_move_to_bottom,
        )

    //endregion

    //region Application
    val Share: ActionSemantic
        get() = ConcreteActionSemantic(
            textRes = R.string.action_share,
            imageVector = Icons.Default.Share,
        )

    val CopyToClipboard: ActionSemantic
        get() = ConcreteActionSemantic(
            textRes = R.string.action_copy_to_clipboard,
        )

    val Scan: ActionSemantic
        get() = ConcreteActionSemantic(
            textRes = R.string.action_scan,
            imageRes = R.drawable.ic_scan_white_24dp,
        )

    val ScanMedia: ActionSemantic
        get() = ConcreteActionSemantic(
            textRes = R.string.action_scan_media,
            imageRes = R.drawable.ic_scan_white_24dp,
        )

    val SetAsStartDirectory: ActionSemantic
        get() = ConcreteActionSemantic(
            textRes = R.string.action_set_as_start_directory,
        )

    val AddToBlackList: ActionSemantic
        get() = ConcreteActionSemantic(
            textRes = R.string.action_add_to_black_list,
        )

    val ThemeSwitch: ActionSemantic
        get() = ConcreteActionSemantic(
            textRes = R.string.action_theme_switch,
            imageRes = R.drawable.ic_theme_switch_white_24dp,
        )

    val ClearAllPreference: ActionSemantic
        get() = ConcreteActionSemantic(
            textRes = R.string.action_clear_all_preference,
        )

    val GrantStoragePermission: ActionSemantic
        get() = ConcreteActionSemantic(
            textRes = R.string.action_grant_storage_permission,
        )

    val ViewExternalFiles: ActionSemantic
        get() = ConcreteActionSemantic(
            textRes = R.string.action_view_external_files,
            imageRes = R.drawable.ic_folder_white_24dp,
        )

    val Reboot: ActionSemantic
        get() = ConcreteActionSemantic(
            textRes = R.string.action_reboot,
        )

    val AddToFavorites: ActionSemantic
        get() = ConcreteActionSemantic(
            textRes = R.string.action_add_to_favorites,
            imageRes = R.drawable.ic_favorite_border_white_24dp,
            imageVector = Icons.Default.FavoriteBorder,
        )

    val RemoveFromFavorites: ActionSemantic
        get() = ConcreteActionSemantic(
            textRes = R.string.action_remove_from_favorites,
            imageRes = R.drawable.ic_favorite_white_24dp,
            imageVector = Icons.Default.Favorite,
        )

    val ChooseLyrics: ActionSemantic
        get() = ConcreteActionSemantic(
            textRes = R.string.action_choose_lyrics,
        )

    val ChangeNowPlayingScreen: ActionSemantic
        get() = ConcreteActionSemantic(
            textRes = R.string.action_change_now_playing_screen,
        )

    val SleepTimer: ActionSemantic
        get() = ConcreteActionSemantic(
            textRes = R.string.action_sleep_timer,
            imageRes = R.drawable.ic_timer_white_24dp,
        )
    //endregion
}
