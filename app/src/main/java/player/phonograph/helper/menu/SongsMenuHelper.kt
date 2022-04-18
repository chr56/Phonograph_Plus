package player.phonograph.helper.menu

import androidx.fragment.app.FragmentActivity
import player.phonograph.R
import player.phonograph.dialogs.AddToPlaylistDialog
import player.phonograph.dialogs.DeleteSongsDialog
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.model.Song

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