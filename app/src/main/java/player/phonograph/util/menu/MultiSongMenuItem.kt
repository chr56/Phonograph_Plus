/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.util.menu

import androidx.fragment.app.FragmentActivity
import player.phonograph.R
import player.phonograph.dialogs.AddToPlaylistDialog
import player.phonograph.dialogs.DeleteSongsDialog
import player.phonograph.model.Song
import player.phonograph.service.MusicPlayerRemote

/**
 * @param activity   The activity who has the menu that call this funcyion
 * @param songs      Songs to process
 * @param menuItemId ItemId in menu as well as `Unique Action ID`
 */
fun onMultiSongMenuItemClick(activity: FragmentActivity, songs: List<Song>, menuItemId: Int): Boolean {
    when (menuItemId) {
        R.id.action_play -> {
            MusicPlayerRemote.playNow(songs)
            return true
        }
        R.id.action_play_next -> {
            MusicPlayerRemote.playNext(songs)
            return true
        }
        R.id.action_add_to_current_playing -> {
            MusicPlayerRemote.enqueue(songs)
            return true
        }
        R.id.action_add_to_playlist -> {
            AddToPlaylistDialog.create(songs)
                .show(activity.supportFragmentManager, "ADD_PLAYLIST")
            return true
        }
        R.id.action_delete_from_device -> {
            DeleteSongsDialog.create(ArrayList(songs))
                .show(activity.supportFragmentManager, "DELETE_SONGS")
            return true
        }
    }
    return false
}
