package com.kabouzeid.gramophone.helper.menu

import androidx.fragment.app.FragmentActivity
import com.kabouzeid.gramophone.R
import com.kabouzeid.gramophone.dialogs.AddToPlaylistDialog
import com.kabouzeid.gramophone.dialogs.DeleteSongsDialog
import com.kabouzeid.gramophone.helper.MusicPlayerRemote
import com.kabouzeid.gramophone.model.Song

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
object SongsMenuHelper {
    @JvmStatic
    fun handleMenuClick(activity: FragmentActivity, songs: List<Song>, menuItemId: Int): Boolean {
        when (menuItemId) {
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
                DeleteSongsDialog.create(songs)
                    .show(activity.supportFragmentManager, "DELETE_SONGS")
                return true
            }
        }
        return false
    }
}