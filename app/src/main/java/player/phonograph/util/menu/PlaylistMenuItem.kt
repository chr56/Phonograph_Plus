/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.util.menu

import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import player.phonograph.R
import player.phonograph.dialogs.AddToPlaylistDialog
import player.phonograph.dialogs.ClearPlaylistDialog
import player.phonograph.dialogs.RenamePlaylistDialog
import player.phonograph.misc.SAFCallbackHandlerActivity
import player.phonograph.model.playlist.Playlist
import player.phonograph.service.MusicPlayerRemote
import util.phonograph.m3u.PlaylistsManager

/**
 * @param activity   The activity who has the menu that call this funcyion
 * @param playlist   Playlist to process
 * @param menuItemId ItemId in menu as well as `Unique Action ID`
 */
fun onPlaylistMenuItemClick(activity: FragmentActivity, playlist: Playlist, menuItemId:Int): Boolean {
    when (menuItemId) {
        R.id.action_play -> {
            MusicPlayerRemote.openQueue(
                ArrayList(playlist.getSongs(activity)), 0, true
            )
            return true
        }
        R.id.action_play_next -> {
            MusicPlayerRemote.playNext(ArrayList(playlist.getSongs(activity)))
            return true
        }
        R.id.action_add_to_current_playing -> {
            MusicPlayerRemote.enqueue(ArrayList(playlist.getSongs(activity)))
            return true
        }
        R.id.action_add_to_playlist -> {
            AddToPlaylistDialog.create(playlist.getSongs(activity))
                .show(activity.supportFragmentManager, "ADD_PLAYLIST")
            return true
        }
        R.id.action_rename_playlist -> {
            RenamePlaylistDialog.create(playlist.id).show(activity.supportFragmentManager, "RENAME_PLAYLIST")
            return true
        }
        R.id.action_delete_playlist, R.id.action_clear_playlist -> {
            ClearPlaylistDialog.create(listOf(playlist)).show(activity.supportFragmentManager, "CLEAR_PLAYLIST")
            return true
        }
        R.id.action_save_playlist -> {
            CoroutineScope(Dispatchers.Default).launch {

                if (activity is SAFCallbackHandlerActivity) {
                    PlaylistsManager(activity, activity).duplicatePlaylistViaSaf(playlist)
                } else {
                    PlaylistsManager(activity, null).duplicatePlaylistViaSaf(playlist)
                }
            }
            return true
        }
    }
    return false
}